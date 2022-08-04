package weco.concepts.ingestor

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.scaladsl.Sink
import grizzled.slf4j.Logging
import weco.concepts.ingestor.model.IdentifierType
import weco.concepts.ingestor.stages.{Fetcher, Scroll, Transformer}

import scala.concurrent.{ExecutionContext, Future}

class IngestStream(dataUrl: String)(implicit actorSystem: ActorSystem)
    extends Logging {
  implicit val executionContext: ExecutionContext = actorSystem.dispatcher

  lazy val fetcher = new Fetcher(Http().superPool())

  def run: Future[Done] =
    fetcher
      .fetchFromUrl(dataUrl)
      .via(Scroll.apply)
      .via(Transformer.apply[IdentifierType.LCSubjects.type])
      .runWith(
        Sink.fold(0L)((nLines, _) => nLines + 1)
      )
      .map(nLines => {
        info(s"Transformed $nLines lines from $dataUrl")
        Done
      })
}
