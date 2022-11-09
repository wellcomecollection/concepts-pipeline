package weco.concepts.ingestor

import grizzled.slf4j.Logging

import scala.util.{Failure, Success}

object Main extends App with IngestorMain with Logging {
  ingestStream.run
    .recover(err => error(err.getMessage))
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
