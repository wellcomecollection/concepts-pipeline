package weco.concepts.ingestor

import akka.Done
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import grizzled.slf4j.Logging

import scala.concurrent.Future

object IngestStream extends Logging {
  def run(dataUrl: String)(implicit as: ActorSystem): Future[Done] =
    Source.single(dataUrl).runForeach(url => info(s"Fetching URL: $url"))
}
