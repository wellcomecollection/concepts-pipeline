package weco.concepts.aggregator

import akka.NotUsed
import akka.actor.ActorSystem
import weco.concepts.aggregator.Main.workUrlTemplate
import weco.concepts.common.model.UsedConcept

import scala.io.{Source => IoSource}
import akka.stream.scaladsl.{Flow, Source => AkkaSource}

import scala.util.{Failure, Success, Using}

class WorkIdAggregator(workIds:Iterator[String])
                      (implicit actorSystem: ActorSystem)
  extends ConceptsAggregator {

  override protected def conceptSource: AkkaSource[UsedConcept, NotUsed] = {
    AkkaSource.fromIterator(
      () => workIds.iterator
    ).via(
      Flow.fromFunction(ConceptsFromWorkId)
    ).mapConcat(identity)
  }

  private def ConceptsFromWorkId(workId: String): Seq[UsedConcept] = {
    Using(IoSource.fromURL(workUrlTemplate.format(workId))) {
      source => source.mkString
    } match {
      case Success(jsonString) =>
        ConceptExtractor(jsonString)
      case Failure(exception) =>
        error(s"could not fetch $workId, $exception")
        Nil
    }
  }
}
