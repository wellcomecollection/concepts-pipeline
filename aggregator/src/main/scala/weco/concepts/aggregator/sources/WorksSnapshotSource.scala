package weco.concepts.aggregator.sources

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.scaladsl.Source
import weco.concepts.common.source.{Fetcher, Scroll}

object WorksSnapshotSource {

  def apply(
    dataUrl: String
  )(implicit actorSystem: ActorSystem): Source[String, NotUsed] = {
    lazy val fetcher = new Fetcher(Http().superPool())
    fetcher
      .fetchFromUrl(dataUrl)
      .via(Scroll.fromCompressed(512 * 1024))
  }
}
