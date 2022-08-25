package weco.concepts.aggregator
import akka.actor.ActorSystem
import akka.{Done, NotUsed}
import akka.stream.scaladsl.{Flow, Sink, Source}
import grizzled.slf4j.Logging
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import weco.concepts.common.model.UsedConcept

import scala.collection.mutable.{Set => MutableSet}
import scala.concurrent.{ExecutionContext, Future}

/** Aggregate Concepts from JSON strings emitted by jsonSource
  */
class ConceptsAggregator(
  jsonSource: Source[String, NotUsed],
  indexName: String,
  maxRecordsPerBulkRequest: Int
)(implicit
  actorSystem: ActorSystem
) extends Logging {
  implicit val executionContext: ExecutionContext = actorSystem.dispatcher

  private val elasticClient: RestClient = {
    // Currently points to an insecure local database.
    // TODO: Do it properly, with details from config/environment
    // once plumbed in to a persistent DB
    val hostname = "elasticsearch"
    val port = 9200
    val scheme = "http"
    RestClient
      .builder(new HttpHost(hostname, port, scheme))
      .setCompressionEnabled(true)
      .build()
  }

  private val bulkUpdateFlow = new BulkUpdateFlow(
    formatter = new BulkFormatter(indexName).format,
    // Although (theoretically) it may be possible to send the whole set in one
    // bulk request (The 22 Aug snapshot amounts to 17MB of documents once
    // deduplicated), it's prudent to break this up in order to ensure stability.
    // Running locally (not perfectly scientific), against a prepopulated
    // elasticsearch index in Docker, (time cat ~/Downloads/works.json| docker
    // compose run -T aggregator) peak speed seemed to be at 50K documents
    // (3m30s). indexing in 25 and 100K batches both took about 3m45s
    max_bulk_records = maxRecordsPerBulkRequest,
    elasticClient = elasticClient
  ).flow

  def run: Future[Done] =
    conceptSource
      .via(deduplicateFlow)
      .via(bulkUpdateFlow)
      .runWith(
        Sink.fold(0L)((acc, conceptCounts) => acc + conceptCounts("total"))
      )
      .map(nConcepts => {
        info(s"Extracted $nConcepts unique concepts")
        elasticClient.close()
        Done
      })

  private def conceptSource: Source[UsedConcept, NotUsed] = {
    jsonSource
      .via(extractConceptsFlow)
      .mapConcat(identity)
  }

  private def extractConceptsFlow: Flow[String, Seq[UsedConcept], NotUsed] =
    Flow.fromFunction(ConceptExtractor.apply)

  /** Remove duplicate concepts from the stream.
    *
    * Here is a probable batch mode/single mode diversion. In single mode,
    * entries are already deduplicated by the ConceptExtractor and the size of a
    * single request will be nowhere near the limits imposed by a batch DB
    * update The whole lot should just be stuffed in the database.
    *
    * In batch mode, concepts returned by different works will need to be
    * deduplicated before inserting, and we will need to take care to ensure
    * that updates are bundled into an appropriate size for insertion.
    *
    * However, the process of "deduplicating" on a small and already unique set
    * of Concepts will be pretty lightweight, so we might as well do it, instead
    * of devising a separate path to avoid it.
    *
    * Elasticsearch Bulk API is clever enough to be able to handle duplicate
    * entries appropriately but given the scale of duplication in the full set,
    * deduplicating here is likely to be more efficient, even just in saving the
    * amount of data we have to send. (In the 2022-08-22 snapshot I found over
    * 3.7M concepts, but fewer than 0.25M different concepts)
    */
  def deduplicateFlow: Flow[UsedConcept, UsedConcept, NotUsed] =
    Flow[UsedConcept].statefulMapConcat { () =>
      val seen: MutableSet[String] = MutableSet.empty[String];
      { concept: UsedConcept =>
        val id = concept.identifier.toString
        if (seen.add(id)) Some(concept) else None
      }
    }

}
