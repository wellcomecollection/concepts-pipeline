package weco.concepts.aggregator.sources

import akka.NotUsed
import akka.stream.scaladsl.Source
import akka.util.ByteString
import weco.concepts.common.source.Scroll

object StdInSource {
  def apply(): Source[String, NotUsed] = {
    Source(
      // Produce a single ByteString, in the same fashion
      // as the Fetcher
      Seq(ByteString(System.in.readAllBytes()))
    ).via(Scroll.fromUncompressed(512 * 1024))
  }
}