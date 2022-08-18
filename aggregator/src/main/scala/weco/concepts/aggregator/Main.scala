package weco.concepts.aggregator

import com.typesafe.config.ConfigFactory
import grizzled.slf4j.Logging
import net.ceedubs.ficus.Ficus._

import scala.concurrent.ExecutionContext
import akka.actor.ActorSystem
import weco.concepts.aggregator.sources.{StdInSource, WorksSnapshotSource}

object Main extends App with Logging {
  val config = ConfigFactory.load()
  lazy val snapshotUrl = config.as[String]("data-source.works.snapshot")
  lazy val workUrlTemplate = config.as[String]("data-source.workURL.template")
  lazy val maxFrameKiB = config.as[Int]("data-source.maxframe.kib")
  implicit val actorSystem: ActorSystem = ActorSystem("main")
  implicit val executionContext: ExecutionContext = actorSystem.dispatcher

  // If you give it ids, it will fetch those records individually
  // If you don't it will either look at stdin or fetch the snapshot.
  val aggregator =
    if (args.length > 0) new WorkIdAggregator(args.iterator)
    else AggregatorFromNDJSonSource

  aggregator.run
    .recover(err => error(err.getMessage))
    .onComplete(_ => actorSystem.terminate())

  private def AggregatorFromNDJSonSource: ConceptsAggregator = {
    val maxFrameBytes = maxFrameKiB * 1024
    val source =
      if (System.in.available() > 0) StdInSource(maxFrameBytes)
      else WorksSnapshotSource(snapshotUrl, maxFrameBytes)
    new NDJSonAggregator(source)
  }

}
