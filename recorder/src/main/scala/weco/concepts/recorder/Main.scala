package weco.concepts.recorder

import akka.stream.scaladsl.{Sink, Source}
import grizzled.slf4j.Logging
import weco.concepts.common.elasticsearch.BulkUpdateResult

object Main extends RecorderMain with Logging with App {
  indices
    .create(targetIndex)
    .map { _ =>
      if (args.length > 0) {
        Source.fromIterator(() => args.iterator).via(recorderStream.recordIds)
      } else {
        recorderStream.recordAllUsedConcepts
      }
    }
    .flatMap {
      _.runWith(Sink.fold[Long, BulkUpdateResult](0L)(_ + _.updated.size))
        .map { total =>
          info(s"Recorded $total concepts")
        }
    }
    .onComplete(_ => {
      actorSystem.terminate()
    })
}
