package weco.concepts.aggregator

import akka.NotUsed
import akka.stream.scaladsl.Flow
import grizzled.slf4j.Logging
import org.elasticsearch.client.{Request, Response, RestClient}

/*
 * An Akka Flow that takes a stream of objects and inserts them into ElasticSearch
 * Using the bulk API
 * The stream emits a map of actions taken (created, updated, noop), to counts of records that
 * experienced that action.
 * TODO: I'm not sure what is the best thing to emit.  I think this is reusable by the ingestor,
 *  and that may need to know about specific changes made.  The aggregator doesn't care.
 *  the output is just ot allow it to report on some numbers.
 */

class BulkUpdateFlow[T](
  formatter: T => String,
  max_bulk_records: Int,
  elasticClient: RestClient
) extends Logging {

  def flow: Flow[T, Map[String, Int], NotUsed] = {
    Flow
      .fromFunction(formatter)
      .grouped(max_bulk_records)
      .via(sendBulkUpdateFlow)
      .via(Flow.fromFunction(countActions))
  }

  /** Given a stream of batches of BulkAPI action/document pairs, post them to
    * Elasticsearch, emitting the responses
    */

  private def sendBulkUpdateFlow: Flow[Seq[String], Response, NotUsed] = {
    def fn(couplets: Seq[String]): Response = {
      info(s"indexing ${couplets.length} concepts")
      val rq = new Request("post", "_bulk")
      rq.setJsonEntity(couplets.mkString(start = "", sep = "\n", end = "\n"))
      // running synchronously, because the very next step is to examine the response
      // to work out what ES did with the data we provided.
      // This also stops us rapidly posting a bunch of bulk updates while ES is still
      // trying to work out what to do with the last three we sent it
      elasticClient.performRequest(rq)
    }
    Flow.fromFunction(fn)

  }

  /** Given a Response from a BulkAPI call, log what it did. Emit the
    * created/updated/noop counts as a Map
    */
  private def countActions(
    response: Response
  ): Map[String, Int] = {
    // The happy path is assumed here for now. Future development
    // might look at different handling of failure modes.
    //
    // We could check Response for success/fail before continuing,
    // but if there's a failure to connect (404, 500), then
    // it will fail at ujson.read anyway.
    // First, log the broad response details (url, response code).
    // That should always succeed, regardless of what ES has done.
    // A 400 error will include some useful info in response.toString.
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
