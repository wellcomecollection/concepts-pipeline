package weco.concepts.ingestor

import akka.Done
import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import grizzled.slf4j.Logging
import weco.concepts.ingestor.stages.Fetcher

import scala.concurrent.{ExecutionContext, Future}

class IngestStream(dataUrl: String)(implicit actorSystem: ActorSystem)
    extends Logging {
  implicit val executionContext: ExecutionContext = actorSystem.dispatcher

  lazy val fetcher = new Fetcher()

  def run: Future[Done] =
    fetcher
      .fetchFromUrl(dataUrl)
      .runWith(
        // Count the number of bytes we've seen
        // This is just while future stages don't exist
        Sink.fold[(Int, Long), ByteString]((0, 0L)) {
          case ((nChunks, total), byteString) =>
            val length = byteString.length
            (nChunks + 1, total + length)
        }
      )
      .map { case (nChunks, totalBytes) =>
        info(s"Streamed $totalBytes bytes in $nChunks chunks")
        Done
      }
}
