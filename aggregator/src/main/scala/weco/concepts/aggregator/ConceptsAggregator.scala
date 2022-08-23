package weco.concepts.aggregator
import akka.actor.ActorSystem
import akka.{Done, NotUsed}
import akka.stream.scaladsl.{Flow, Sink, Source}
import grizzled.slf4j.Logging
import weco.concepts.common.model.UsedConcept

import scala.concurrent.{ExecutionContext, Future}

/** Aggregate Concepts from JSON strings emitted by jsonSource
  */

class ConceptsAggregator(jsonSource: Source[String, NotUsed])(implicit
  actorSystem: ActorSystem
) extends Logging {
  implicit val executionContext: ExecutionContext = actorSystem.dispatcher

  def run: Future[Done] =
    conceptSource
      .via(deduplicateFlow)
      .via(prepareBulkBodyFlow)
      .via(sendBulkUpdateFlow)
      .runWith(
        Sink.fold(0L)((nConcepts, _) => nConcepts + 1)
      )
      .map(nConcepts => {
        info(s"Extracted $nConcepts concepts")
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
    * deduplicated before inserting, and we may need to take care to ensure that
    * updates are bundled into an optimal size for insertion. However, the
    * process of "deduplicating" on a small and already unique set of Concepts
    * will be pretty lightweight, so we might as well do it, instead of devising
    * a separate path to avoid it.
    *
    * Elasticsearch Bulk API is clever enough to be able to handle duplicate
    * entries appropriately but given the scale of duplication in the full set,
    * deduplicating here is likely to be more efficient, even just in saving the
    * amount of data we have to send. (At time of writing, I found over 3.7M
    * concepts, but fewer than 0.25M different concepts)
    */
  private def deduplicateFlow: Flow[UsedConcept, UsedConcept, NotUsed] =
    Flow[UsedConcept].statefulMapConcat { () =>
      var seen: Set[String] = Set.empty[String]
      val dedupe = { concept: UsedConcept =>
        val id = concept.identifier.toString
        val returnValue =
          if (seen.contains(id)) None else Some(concept)
        seen += id
        returnValue
      }
      dedupe
    }

  /** Turn a stream of concepts into batched sequences of Update Requests to
    * send to Elastic.
    *
    * Although, theoretically, it may be possible to send the whole set in one
    * bulk request (The 22 Aug snapshot amounts to 17MB of documents once
    * deduplicated), it's prudent to break this up in order to ensure stability.
    */
  private def prepareBulkBodyFlow: Flow[UsedConcept, Seq[String], NotUsed] =
    Flow.fromFunction(new BulkFormatter("my_index").format).grouped(50000)

  private def sendBulkUpdateFlow: Flow[Seq[String], Unit, NotUsed] = {
    // TODO: Actually put it in a database.

    // Because the expected endpoint for all this is a list of all concepts currently
    // in use in the input data, it might be appropriate to always UPDATE records
    // and ignore any failures due to clashes.  However, I recall that ES can get
    // a bit miffed if you try to do conflicting things in one bulk request.
    Flow.fromFunction(println)
  }

}
