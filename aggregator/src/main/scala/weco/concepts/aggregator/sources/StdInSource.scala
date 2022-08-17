package weco.concepts.aggregator.sources

import akka.NotUsed
import akka.stream.scaladsl.{Source, StreamConverters}
import grizzled.slf4j.Logging
import weco.concepts.common.source.Scroll

/**
 * Scroll over lines from stdin.
 */

object StdInSource extends Logging{

  def apply(maxFrameBytes:Int): Source[String, NotUsed] = {
    info("reading from stdin")
    StreamConverters.fromInputStream(
      () => System.in
    ).via(Scroll.fromUncompressed(maxFrameBytes)).asInstanceOf[Source[String, NotUsed]]
  }
}