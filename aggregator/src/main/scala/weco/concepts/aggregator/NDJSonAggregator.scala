package weco.concepts.aggregator

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Source}
import weco.concepts.common.model.UsedConcept

class NDJSonAggregator(jsonSource: Source[String, NotUsed])(implicit
                                                            actorSystem: ActorSystem
) extends ConceptsAggregator{

  override protected def conceptSource: Source[UsedConcept, NotUsed] = {
    jsonSource
      .via(extractConceptsFlow)
      .mapConcat(identity)
  }

  private def extractConceptsFlow: Flow[String, Seq[UsedConcept], NotUsed] =
    Flow.fromFunction(ConceptExtractor.apply)

}
