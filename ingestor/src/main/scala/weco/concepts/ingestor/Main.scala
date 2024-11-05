package weco.concepts.ingestor

import grizzled.slf4j.Logging
import org.apache.pekko.Done

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

object Main extends App with IngestorMain with Logging {
  val future: Future[Done] = ingestStream.run
    .recover { case err: Throwable =>
      error(err.getMessage)
      Done
    }

  future.onComplete { result =>
    result match {
      case Success(_) =>
        info("Execution completed successfully")
      case Failure(exception) =>
        error(s"Execution failed with $exception")
    }
    actorSystem.terminate()
  }

  Await.result(actorSystem.whenTerminated, Duration.Inf)
}
