package weco.concepts.aggregator
import akka.actor.ActorSystem
import akka.{Done, NotUsed}
import akka.stream.scaladsl.{Broadcast, Flow, Keep, Sink, Source}
import grizzled.slf4j.Logging
import weco.concepts.common.elasticsearch.{
  BulkUpdateFlow,
  BulkUpdateResult,
  ElasticHttpClient,
  Indices
}
import weco.concepts.common.model.CatalogueConcept

import scala.collection.mutable.{Set => MutableSet}
import scala.concurrent.{ExecutionContext, Future}

/** Aggregate Concepts from JSON strings emitted by jsonSource
  */
class ConceptsAggregator(
  elasticHttpClient: ElasticHttpClient,
  topicPublisher: TopicPublisher,
  indexName: String,
  maxRecordsPerBulkRequest: Int
)(implicit
  actorSystem: ActorSystem
) extends Logging {
  implicit val executionContext: ExecutionContext = actorSystem.dispatcher
  private val bulkUpdateFlow = new BulkUpdateFlow[CatalogueConcept](
    elasticHttpClient = elasticHttpClient,
    maxBulkRecords = maxRecordsPerBulkRequest,
    indexName = indexName
  ).flow
  private val indices = new Indices(elasticHttpClient)

  def run(jsonSource: Source[String, NotUsed]): Future[Done] = {
    indices.create(indexName).flatMap { _ =>
      conceptSource(jsonSource)
        .via(deduplicateFlow)
        .via(bulkUpdateFlow)
        .runWith(
          Sink.combineMat(
            publishIds,
            Sink.fold[AggregationStats, BulkUpdateResult](
              AggregationStats.empty
            )(_ + _)
          )(
            Broadcast[BulkUpdateResult](_)
          )(_ zip _)
        )
        .map { case (done, stats) =>
          info(stats.summarise)
          done
        }
    }
  }

  private def conceptSource(
    jsonSource: Source[String, NotUsed]
  ): Source[CatalogueConcept, NotUsed] = {
    jsonSource
      .via(extractConceptsFlow)
      .mapConcat(identity)
  }

  private def extractConceptsFlow: Flow[String, Seq[CatalogueConcept], NotUsed] =
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
  private def deduplicateFlow: Flow[CatalogueConcept, CatalogueConcept, NotUsed] =
    Flow[CatalogueConcept].statefulMapConcat { () =>
      val seen: MutableSet[Int] = MutableSet.empty[Int];
      { concept: CatalogueConcept =>
        val id = concept.identifier.hashCode()
        if (seen.add(id)) Some(concept) else None
      }
    }

  private def publishIds: Sink[BulkUpdateResult, Future[Done]] =
    Flow[BulkUpdateResult]
      .mapConcat(_.updated)
      .toMat(topicPublisher.sink)(Keep.right)

  private case class AggregationStats(updated: Long, noop: Long) {
    lazy val total: Long = updated + noop

    def +(nextResult: BulkUpdateResult): AggregationStats = AggregationStats(
      updated = updated + nextResult.updated.size,
      noop = noop + nextResult.noop.size
    )

    def summarise: String =
      s"""Extracted $total distinct concepts ($updated of which had been updated)"""
  }

  private object AggregationStats {
    def empty: AggregationStats = AggregationStats(0L, 0L)
  }

}
