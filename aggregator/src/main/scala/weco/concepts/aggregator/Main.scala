package weco.concepts.aggregator

import akka.NotUsed
import grizzled.slf4j.Logging
import akka.stream.scaladsl.Source
import weco.concepts.aggregator.sources._

object Main extends AggregatorMain with Logging with App {
  // If you give it ids, it will fetch those records individually
  // If you don't it will either look at stdin or fetch the snapshot.

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
    .onComplete(_ => {
      actorSystem.terminate()
    })
}
