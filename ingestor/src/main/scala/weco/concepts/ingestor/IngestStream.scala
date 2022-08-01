package weco.concepts.ingestor

import akka.Done
import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import grizzled.slf4j.Logging
import weco.concepts.ingestor.stages.Fetcher

import scala.concurrent.Future

class IngestStream(dataUrl: String)(implicit actorSystem: ActorSystem)
    extends Logging {
  lazy val fetcher = new Fetcher()

  def run: Future[Done] =
    fetcher
      .fetchFromUrl(dataUrl)
      .runWith(Sink.ignore)
}
