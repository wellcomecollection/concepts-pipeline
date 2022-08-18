package weco.concepts.aggregator.sources

import akka.NotUsed
import akka.stream.scaladsl.{Source, StreamConverters}
import grizzled.slf4j.Logging
import weco.concepts.common.source.Scroll
import weco.concepts.aggregator.Main.maxFrameKiB
/** Scroll over lines from stdin.
  */

object StdInSource extends Logging {

  def apply: Source[String, NotUsed] = {
    info("reading from stdin")
    StreamConverters
      .fromInputStream(() => System.in)
      .via(Scroll.fromUncompressed(maxFrameKiB * 1024))
      .asInstanceOf[Source[String, NotUsed]]
  }
}
