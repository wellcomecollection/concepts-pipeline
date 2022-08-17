package weco.concepts.common.source

import akka.NotUsed
import akka.stream.scaladsl.{Compression, Flow, Framing}
import akka.util.ByteString

object Scroll {
  def fromUncompressed(
    maximumFrameLength: Int
  ): Flow[ByteString, String, NotUsed] =
    Framing
      .delimiter(
        delimiter = ByteString("\n"),
        maximumFrameLength = maximumFrameLength,
        // This confusingly named parameter means that the final line does not need to terminate with a newline
        allowTruncation = true
      )
      .map(_.utf8String)

  def fromCompressed(
    maximumFrameLength: Int
  ): Flow[ByteString, String, NotUsed] =
    Compression
      .gunzip()
      .via(fromUncompressed(maximumFrameLength))

  def apply(maximumFrameLength: Int): Flow[ByteString, String, NotUsed] =
    fromCompressed(maximumFrameLength)

}
