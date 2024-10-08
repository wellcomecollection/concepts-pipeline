package weco.concepts.aggregator
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.{Done, NotUsed}
import org.apache.pekko.stream.scaladsl.{Flow, Keep, Sink, Source}
import grizzled.slf4j.Logging
import weco.concepts.common.elasticsearch.{
  BulkUpdateResult,
  ElasticHttpClient,
  Indices,
  ScriptedBulkUpdateFlow,
  Scripts
}
import weco.concepts.common.model.CatalogueConcept

import scala.collection.mutable.{Set => MutableSet}
import scala.concurrent.{ExecutionContext, Future}

/** Aggregate Concepts from JSON strings emitted by jsonSource
  */
class ConceptsAggregator(
  elasticHttpClient: ElasticHttpClient,
  updatesSink: Sink[String, Future[Done]],
  indexName: String,
  maxRecordsPerBulkRequest: Int,
  shouldUpdateAppenderScript: Boolean
)(implicit
  actorSystem: ActorSystem
) extends Logging {
  implicit val executionContext: ExecutionContext = actorSystem.dispatcher
  private val updateScriptName = "append-fields"
  private val bulkUpdateFlow = new ScriptedBulkUpdateFlow[CatalogueConcept](
    elasticHttpClient = elasticHttpClient,
    maxBulkRecords = maxRecordsPerBulkRequest,
    indexName = indexName,
    updateScriptName
  ).flow

  private val notInIndexFlow = new NotInIndexFlow(
    elasticHttpClient = elasticHttpClient,
    indexName = indexName
  ).flow

  private val indices = new Indices(elasticHttpClient)
  private val scripts = new Scripts(elasticHttpClient)

  def uploadAppenderScript: Future[Done] =
    if (shouldUpdateAppenderScript) scripts.create(updateScriptName, "update")
    else Future(Done)

  def run(jsonSource: Source[String, NotUsed]): Future[Done] = {
    // Store the script in the update context.
    // For some reason, if you store it without context, it will
    // recompile on each use, and then fail because you are running too many
    // script compilations during a bulk update.
    uploadAppenderScript.flatMap { _ =>
      indices.create(indexName).flatMap { _ =>
        conceptSource(jsonSource)
          .via(deduplicateFlow)
          // At bulk scale, scripted updates are prohibitively slow.
          // It is much quicker to first check if the canonicalId is present
          // before attempting to send it to ES.
          //
          // This has no effect when indexing into a pristine database.
          // The use of deduplicateFlow, above, means that no duplicate ids
          // will be found by notInIndexFlow.
          .via(notInIndexFlow)
          // The script used by this bulkUpdate is idempotent, so the use of
          // notIndexFlow is only an optimisation.  At individual scale
          // it may be unnecessary, and will be marginally slower when
          // indexing new Concepts only.
          .via(bulkUpdateFlow)
          .runWith(publishIds)
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

  private def extractConceptsFlow
    : Flow[String, Seq[CatalogueConcept], NotUsed] =
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
  private def deduplicateFlow
    : Flow[CatalogueConcept, CatalogueConcept, NotUsed] =
    Flow[CatalogueConcept].statefulMapConcat { () =>
      val seen: MutableSet[Int] = MutableSet.empty[Int];
      { concept: CatalogueConcept =>
        val id = concept.canonicalId.hashCode
        if (seen.add(id)) Some(concept) else None
      }
    }

  private def publishIds: Sink[BulkUpdateResult, Future[Done]] =
    Flow[BulkUpdateResult]
      .mapConcat(_.updated)
      .toMat(updatesSink)(Keep.right)

}
