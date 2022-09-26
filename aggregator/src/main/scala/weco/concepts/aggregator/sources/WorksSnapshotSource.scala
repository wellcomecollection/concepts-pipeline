package weco.concepts.aggregator.sources

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.scaladsl.Source
import grizzled.slf4j.Logging
import weco.concepts.common.source.{Fetcher, Scroll}

/** Download the Works snapshot and scroll over the lines in it.
  */

object WorksSnapshotSource extends Logging {

  def apply(
    maxFrameKiB: Int,
    dataUrl: String
  )(implicit actorSystem: ActorSystem): Source[String, NotUsed] = {
    info(s"reading from $dataUrl")
    lazy val fetcher = new Fetcher(Http().superPool())
    fetcher
      .fetchFromUrl(dataUrl)
      .via(Scroll.fromCompressed(maxFrameKiB * 1024))
  }
}
