package weco.concepts.aggregator

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.scaladsl.{Flow, Sink}
import grizzled.slf4j.Logging
import weco.concepts.common.model.UsedConcept
import weco.concepts.common.source.{Fetcher, Scroll}

import scala.concurrent.{ExecutionContext, Future}

class AggregateStream(dataUrl: String) (implicit actorSystem: ActorSystem) extends Logging {
  implicit val executionContext: ExecutionContext = actorSystem.dispatcher

  lazy val fetcher = new Fetcher(Http().superPool())
  def run: Future[Done] = {
    // TODO: Move this bit to its own function, somewhere else
    // That will allow me to inject it so that I can run this stream
    // over STDIN.
  fetcher
    .fetchFromUrl(dataUrl)
    // At time of writing, the largest JSON in the catalogue is ca 475KiB
    // 0.5 MiB should give sufficient overhead to allow for expansion in the
    // longest catalogue entries.
    // When running over the whole catalogue like this, it will be essentially under
    // human control, so the number can be tweaked if required, rather than causing a
    // failure in an unsupervised system.
    .via(Scroll(512 * 1024))
    // TODO END
    .via(extractConceptsFlow)
//    .via(saveConceptsFlow)
    // TODO: Also report on the number of concepts
    .runWith(
      Sink.fold(0L)((nLines, _) => nLines + 1)
    )
    .map(nLines => {
      info(s"Transformed $nLines lines from $dataUrl")
      Done
    })
  }

  def extractConceptsFlow: Flow[String, Seq[UsedConcept], NotUsed] =
    Flow.fromFunction(ConceptExtractor.apply)

  def saveConceptsFlow: Flow[Seq[UsedConcept], Unit, NotUsed] = {
    // TODO: Actually put it in a database.
    Flow.fromFunction(println)
  }
}
