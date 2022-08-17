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
      // At time of writing, the largest JSON in the catalogue is ca 475KiB
      // 0.5 MiB should give sufficient overhead to allow for expansion in the
      // longest catalogue entries.
      // When running over the whole catalogue like this, it will be essentially under
      // human control, so the number can be tweaked if required, rather than causing a
      // failure in an unsupervised system.
      .via(Scroll.fromCompressed(512 * 1024))
  }
}
