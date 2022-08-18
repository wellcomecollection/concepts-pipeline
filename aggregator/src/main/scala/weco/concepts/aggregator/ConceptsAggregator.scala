package weco.concepts.aggregator

import akka.actor.ActorSystem
import akka.{Done, NotUsed}
import akka.stream.scaladsl.{Flow, Sink, Source}
import grizzled.slf4j.Logging
import weco.concepts.common.model.UsedConcept

import scala.concurrent.{ExecutionContext, Future}

abstract class ConceptsAggregator
  (implicit actorSystem: ActorSystem)
  extends Logging{
  protected def conceptSource: Source[UsedConcept, NotUsed]
  implicit val executionContext: ExecutionContext = actorSystem.dispatcher
  def run: Future[Done] =
    conceptSource
      //      .via(saveConceptsFlow)
      .runWith(
        Sink.fold(0L)((nConcepts, _) => nConcepts + 1)
      )
      .map(nConcepts => {
        info(s"Extracted $nConcepts concepts")
        Done
      })

  def saveConceptsFlow: Flow[UsedConcept, Unit, NotUsed] = {
    // TODO: Actually put it in a database.
    // Here is a probable batch mode/single mode diversion.
    // In single mode, entries are already deduplicated by the conceptextractor
    // and the size of a single request will be nowhere near the limits imposed by
    // a batch DB update
    // The whole lot should just be stuffed in the database.

    // In batch mode, concepts returned by different works will need to be deduplicated
    // before inserting, and we may need to take care to ensure that updates are
    // bundled into an optimal size for insertion.

    // Because the expected endpoint for all this is a list of all concepts currently
    // in use in the input data, it might be appropriate to always CREATE records
    // and ignore any failures due to clashes.  However, I recall that ES can get
    // a bit miffed if you try to do conflicting things in one bulk request.

    Flow.fromFunction(println)
  }

}
