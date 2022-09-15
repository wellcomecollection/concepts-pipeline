package weco.concepts.common.elasticsearch

import akka.NotUsed
import akka.stream.scaladsl.Flow
import grizzled.slf4j.Logging
import org.elasticsearch.client.{Response, ResponseException}

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

/** Formatter to turn concepts into couplets for use in an Elasticsearch Bulk
  * update request.
  */
trait BulkFormatter[T] {
  def identifier(item: T): String
  def doc(item: T): ujson.Obj
  def format(index: String)(item: T): String = {
    val action = ujson.Obj(
      "update" -> ujson.Obj(
        "_index" -> index,
        "_id" -> identifier(item)
      )
    )
    val document = ujson.Obj(
      "doc_as_upsert" -> true,
      "doc" -> doc(item)
    )
    s"${action.render()}\n${document.render()}"
  }
}

class BulkUpdateFlow[T](
  formatter: BulkFormatter[T],
  max_bulk_records: Int,
  indexer: Indexer,
  indexName: String
) extends Logging {

  def flow: Flow[T, Map[String, Int], NotUsed] = {
    Flow
      .fromFunction(formatter.format(indexName))
      .grouped(max_bulk_records)
      .via(Flow.fromFunction(sendBulkUpdate))
      .via(Flow.fromFunction(countActions))
  }

  /** Given a stream of batches of BulkAPI action/document pairs, post them to
    * Elasticsearch, emitting the responses
    */

  private def sendBulkUpdate(couplets: Seq[String]): Response = {
    info(s"indexing ${couplets.length} concepts")
    // This runs synchronously, because the very next step is to examine the response
    // to work out what ES did with the data we provided.
    // This also stops us rapidly posting a bunch of bulk updates while ES is still
    // trying to work out what to do with the last three we sent it
    indexer.bulk(couplets) match {
      case Success(response) => response
      case Failure(responseException: ResponseException) =>
        error(
          s"Error response returned when sending bulk update: ${responseException.getResponse}"
        )
        throw responseException
      case Failure(otherException) =>
        error("Unexpected error sending bulk update!")
        throw otherException
    }
  }

  /** Given a Response from a BulkAPI call, log what it did. Emit the
    * created/updated/noop counts as a Map
    */
  private def countActions(
    response: Response
  ): Map[String, Int] = {
    info(response)
    val rsJson = ujson.read(response.getEntity.getContent)
    val items = rsJson.obj("items").arr
    val result_counts: Map[String, Int] =
      items
        .groupBy(_.obj("update").obj("result").value.toString)
        .view
        .mapValues(_.size)
        .toMap + ("total" -> items.length)
    info(result_counts)
    result_counts
  }
}