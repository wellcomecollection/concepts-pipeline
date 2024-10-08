package weco.concepts.aggregator

import org.apache.pekko.{Done, NotUsed}
import grizzled.slf4j.Logging
import org.apache.pekko.stream.scaladsl.{Sink, Source}
import weco.concepts.aggregator.sources._

import scala.concurrent.Future
import scala.util.{Failure, Success}

object Main extends AggregatorMain with Logging with App {
  // If you give it ids, it will fetch those records individually
  // If you don't it will either look at stdin or fetch the snapshot.

  // Running the aggregator locally does not require publishing updates
  override protected val updatesSink: Sink[String, Future[Done]] =
    Sink.ignore

  val source: Source[String, NotUsed] =
    if (args.length > 0) workIdSource(args.iterator)
    else if (System.in.available() > 0) StdInSource(maxFrameKiB)
    else
      WorksSnapshotSource(
        maxFrameKiB,
        snapshotUrl
      )

  aggregator
    .run(source)
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
