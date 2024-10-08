package weco.concepts.recorder

import org.apache.pekko.stream.scaladsl.{Sink, Source}
import grizzled.slf4j.Logging
import weco.concepts.common.elasticsearch.BulkUpdateResult

import scala.util.{Failure, Success}

object Main extends RecorderMain with Logging with App {
  indices
    .create(targetIndex)
    .map { _ =>
      if (args.length > 0) {
        val ids = args.toSeq
        info(s"Recording IDs: ${ids.mkString(", ")}")
        Source(ids).via(recorderStream.recordIds)
      } else {
        info("Recording all catalogue concepts")
        recorderStream.recordAllCatalogueConcepts
      }
    }
    .flatMap {
      _.runWith(Sink.fold[Long, BulkUpdateResult](0L)(_ + _.updated.size))
        .map { total =>
          info(s"Recorded $total concepts")
        }
    }
    .onComplete { result =>
      result match {
        case Success(_) =>
          info("Execution completed successfully")
        case Failure(exception) =>
          error(s"Execution failed with $exception")
      }
      actorSystem.terminate()
    }
}
