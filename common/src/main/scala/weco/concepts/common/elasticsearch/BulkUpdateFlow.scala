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
 *
 * TODO: I'm not sure what is the best thing to emit.  I think this is reusable by the ingestor,
 *  and that may need to know about specific changes made.  The aggregator doesn't care.
 *  the output is just ot allow it to report on some numbers.
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

  def flow: Flow[T, Map[String, Int], NotUsed] =
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
    : Flow[(HttpResponse, Seq[String]), Map[String, Int], NotUsed] =
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
              .map { rsJson =>
                val items = rsJson.obj("items").arr
                val result = items
                  .groupBy(_.obj("update").obj("result").value.toString)
                  .view
                  .mapValues(_.size)
                  .toMap + ("total" -> items.length)

                if (!result.get("total").contains(couplets.size)) {
                  warn("Bulk update executed fewer updates than requested")
                  warn(s"Expected ${couplets.size}, got $result")
                }

                result
              }
        }
      }
      .mapMaterializedValue(_ => NotUsed)

  private def accumulateTotals
    : Flow[Map[String, Int], Map[String, Int], NotUsed] =
    Flow[Map[String, Int]].statefulMapConcat(() => {
      var total = 0L
      (result: Map[String, Int]) => {
        total += result.getOrElse("total", 0)
        info(s"Total documents updated in $indexName: $total")
        Seq(result)
      }
    })
}
