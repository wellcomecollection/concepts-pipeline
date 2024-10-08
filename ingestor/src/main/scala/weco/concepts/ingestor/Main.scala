package weco.concepts.ingestor

import grizzled.slf4j.Logging
import org.apache.pekko.Done

import scala.util.{Failure, Success}

object Main extends App with IngestorMain with Logging {
  ingestStream.run
    .recover { case err: Throwable =>
      error(err.getMessage); Done
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
