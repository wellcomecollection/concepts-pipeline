package weco.concepts.ingestor.stages

import akka.NotUsed
import akka.stream.scaladsl._
import akka.util.ByteString

object Scroll {
  def apply: Flow[ByteString, String, NotUsed] =
    Compression
      .gunzip()
      .via(
        Framing.delimiter(
          delimiter = ByteString("\n"),
          maximumFrameLength = 128 * 1024,
          // This confusingly named parameter means that the final line does not need to terminate with a newline
          allowTruncation = true
        )
      )
      .map(_.utf8String)
}
