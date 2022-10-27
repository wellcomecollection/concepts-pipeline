package weco.concepts.common.elasticsearch

import akka.NotUsed
import akka.http.scaladsl.model._
import akka.stream.scaladsl.Flow
import grizzled.slf4j.Logging
import weco.concepts.common.json.Indexable
import weco.concepts.common.json.Indexable._

import scala.util.{Failure, Success}

/*
 * An Akka Flow that takes a stream of objects and inserts them into ElasticSearch
 * Using the bulk API.
 *
 * The stream emits a map of actions taken (created, updated, noop), to counts of records that
 * experienced that action in each bulk request.
 *
 * Objects in the stream are pushed to Elasticsearch in groups determined by `max_bulk_records`.
 * In reality, the Bulk API is limited by the size (in MiB) of a request, and by the complexity
 * of the index mapping. ES documentation does not give guidance on either the hard or soft limits
 * involved here, because it is very much determined by the nature of the cluster and data.
 * This means that the value of max_bulk_records should be chosen by the author by experimentation.
 *
 * Although (theoretically) it may be possible to send the whole set of used concepts in one
 * bulk request (The 22 Aug snapshot amounts to 17MB of documents, well within "few tens of MB"),
 * it's prudent to break this up in order to ensure stability, and also to improve observability.
 * Saving it all up for one big hit, then sending it all to ES is a recipe for things to go
 * unpredictably wrong, and waiting for three minutes to see nothing happen is quite frustrating.
 */

class BulkUpdateFlow[T: Indexable](
  elasticHttpClient: ElasticHttpClient,
  maxBulkRecords: Int,
  indexName: String,
  filterDocuments: T => Boolean = (_: T) => true
) extends Logging {
  def format(item: T): String = {
    val action = ujson.Obj(
      "update" -> ujson.Obj(
        "_index" -> indexName,
        "_id" -> item.id
      )
    )
    val document = ujson.Obj(
      "doc_as_upsert" -> true,
      "doc" -> item.toDoc
    )
    s"${action.render()}\n${document.render()}"
  }

  def flow: Flow[T, BulkUpdateResult, NotUsed] =
    Flow[T]
      .filter(filterDocuments)
      .via(Flow.fromFunction(format))
      .grouped(maxBulkRecords)
      .via(elasticsearchBulkFlow)
      .via(ElasticAkkaHttpClient.deserializeJson)
      .via(checkResultsFlow)
      .via(accumulateTotals)

  /** Given a stream of batches of BulkAPI action/document pairs, post them to
    * Elasticsearch, emitting the responses and the expected count of updates
    */
  private def elasticsearchBulkFlow: Flow[Seq[String], HttpResponse, NotUsed] =
    Flow[Seq[String]]
      .map { couplets =>
        val requestBody = couplets.mkString(start = "", sep = "\n", end = "\n")
        HttpRequest(
          method = HttpMethods.POST,
          uri = "/_bulk",
          entity = HttpEntity(ContentTypes.`application/json`, requestBody)
        ) -> ()
      }
      .via(elasticHttpClient.flow[Unit])
      .map {
        case (Success(response), _) if response.status.isSuccess() =>
          response
        case (Success(errorResponse), _) =>
          error("Error response returned when sending bulk update")
          throw new RuntimeException(s"Response: $errorResponse")
        case (Failure(exception), _) =>
          error("Unexpected error sending bulk update!")
          throw exception
      }

  /** Given a Response from a BulkAPI call, log what it did. Emit the
    * created/updated/noop counts as a Map
    */
  private def checkResultsFlow: Flow[ujson.Value, BulkUpdateResult, NotUsed] =
    Flow[ujson.Value]
      .map(BulkUpdateResult.apply)
      .map {
        case result if result.errored.nonEmpty =>
          result.errored.foreach { case (id, err) =>
            error(s"Update for $id failed: ${err.render(indent = 2)}")
          }
          throw new RuntimeException(
            s"Bulk update failed for ${result.errored.size} items (succeeded for ${result.total})"
          )
        case success => success
      }

  private def accumulateTotals
    : Flow[BulkUpdateResult, BulkUpdateResult, NotUsed] =
    Flow[BulkUpdateResult].statefulMapConcat(() => {
      var total = 0L
      (result: BulkUpdateResult) => {
        total += result.total
        info(s"Total documents updated in $indexName: $total")
        Seq(result)
      }
    })
}

case class BulkUpdateResult(
  took: Long,
  errored: Map[String, ujson.Value],
  updated: Seq[String],
  noop: Seq[String]
) {
  lazy val total: Int = updated.size + noop.size
}

object BulkUpdateResult {
  import weco.concepts.common.json.JsonOps._
  def apply(responseJson: ujson.Value): BulkUpdateResult = BulkUpdateResult(
    took = responseJson.opt[Long]("took").getOrElse(0L),
    errored = itemIds(
      pred = _.opt[ujson.Value]("error").isDefined,
      transform = item => getId(item) zip item.opt[ujson.Value]("error")
    )(responseJson).toMap,
    updated = itemIds(
      _.opt[String]("result").exists {
        case "updated" => true
        case "created" => true
        case _         => false
      }
    )(responseJson),
    noop = itemIds(_.opt[String]("result").contains("noop"))(responseJson)
  )

  private def getId(item: ujson.Value): Option[String] = item.opt[String]("_id")

  private def itemIds[T](
    pred: ujson.Value => Boolean,
    transform: ujson.Value => Option[T] = getId _
  )(json: ujson.Value): Seq[T] =
    json
      .opt[Seq[ujson.Value]]("items")
      .map {
        _.flatMap(_.opt[ujson.Value]("update"))
          .collect {
            case item if pred(item) => transform(item)
          }
          .flatten
      }
      .getOrElse(Nil)
}
