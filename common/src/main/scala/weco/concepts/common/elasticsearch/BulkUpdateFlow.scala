package weco.concepts.common.elasticsearch

import akka.NotUsed
import akka.http.scaladsl.model._
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import grizzled.slf4j.Logging

import scala.concurrent.ExecutionContext
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

abstract class BulkUpdateFlow[T](
  elasticHttpClient: ElasticHttpClient,
  maxBulkRecords: Int,
  indexName: String
) extends Logging {

  def identifier(item: T): Option[String]
  def doc(item: T): Option[ujson.Obj]

  def format(item: T): Option[String] = for {
    id <- identifier(item)
    source <- doc(item)
  } yield {
    val action = ujson.Obj(
      "update" -> ujson.Obj(
        "_index" -> indexName,
        "_id" -> id
      )
    )
    val document = ujson.Obj(
      "doc_as_upsert" -> true,
      "doc" -> source
    )
    s"${action.render()}\n${document.render()}"
  }

  def flow: Flow[T, BulkUpdateResult, NotUsed] =
    Flow
      .fromFunction(format)
      .collect { case Some(update) => update }
      .grouped(maxBulkRecords)
      .via(elasticsearchBulkFlow)
      .via(checkResultsFlow)
      .via(accumulateTotals)

  /** Given a stream of batches of BulkAPI action/document pairs, post them to
    * Elasticsearch, emitting the responses and the expected count of updates
    */
  private def elasticsearchBulkFlow
    : Flow[Seq[String], (HttpResponse, Seq[String]), NotUsed] =
    Flow[Seq[String]]
      .map { couplets =>
        val requestBody = couplets.mkString(start = "", sep = "\n", end = "\n")
        HttpRequest(
          method = HttpMethods.POST,
          uri = "/_bulk",
          entity = HttpEntity(ContentTypes.`application/json`, requestBody)
        ) -> couplets
      }
      .via(elasticHttpClient.flow[Seq[String]])
      .map {
        case (Success(response), couplets) if response.status.isSuccess() =>
          (response, couplets)
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
  private def checkResultsFlow
    : Flow[(HttpResponse, Seq[String]), BulkUpdateResult, NotUsed] =
    Flow
      .fromMaterializer { (materializer, _) =>
        implicit val mat: Materializer = materializer
        implicit val ec: ExecutionContext = mat.executionContext
        Flow[(HttpResponse, Seq[String])].mapAsyncUnordered(10) {
          case (response, couplets) =>
            response.entity.dataBytes
              .runReduce(_ ++ _)
              .map(_.utf8String)
              .map(ujson.read(_))
              .map(BulkUpdateResult.apply)
              .map { result =>
                if (result.total != couplets.size) {
                  warn("Bulk update executed fewer updates than requested")
                  warn(s"Expected ${couplets.size}, got $result")
                }
                result
              }
        }
      }
      .mapMaterializedValue(_ => NotUsed)

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
  errored: Seq[String],
  updated: Seq[String],
  noop: Seq[String]
) {
  lazy val total: Int = updated.size + noop.size
}

object BulkUpdateResult {
  import weco.concepts.common.json.JsonOps._
  def apply(responseJson: ujson.Value): BulkUpdateResult = BulkUpdateResult(
    took = responseJson.opt[Long]("took").getOrElse(0L),
    errored = itemIds(_.opt[ujson.Value]("error").isDefined)(responseJson),
    updated = itemIds(
      _.opt[String]("result").exists {
        case "updated" => true
        case "created" => true
        case _         => false
      }
    )(responseJson),
    noop = itemIds(_.opt[String]("result").contains("noop"))(responseJson)
  )

  private def itemIds(
    pred: ujson.Value => Boolean
  )(json: ujson.Value): Seq[String] =
    json
      .opt[Seq[ujson.Value]]("items")
      .map {
        _.flatMap(_.opt[ujson.Value]("update"))
          .collect {
            case item if pred(item) => item.opt[String]("_id")
          }
          .flatten
      }
      .getOrElse(Nil)
}
