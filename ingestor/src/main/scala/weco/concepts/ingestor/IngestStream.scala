package weco.concepts.ingestor

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.scaladsl.Sink
import grizzled.slf4j.Logging
import weco.concepts.ingestor.stages.{Fetcher, Scroll}

import scala.concurrent.{ExecutionContext, Future}

class IngestStream(dataUrl: String)(implicit actorSystem: ActorSystem)
    extends Logging {
  implicit val executionContext: ExecutionContext = actorSystem.dispatcher

  lazy val fetcher = new Fetcher(Http().superPool())

  def run: Future[Done] =
    fetcher
      .fetchFromUrl(dataUrl)
      .via(Scroll.apply)
      .runWith(
        Sink.fold(0L)((nLines, _) => nLines + 1)
      )
      .map(nLines => {
        info(s"Read $nLines lines from decompressed $dataUrl")
        Done
      })
}
