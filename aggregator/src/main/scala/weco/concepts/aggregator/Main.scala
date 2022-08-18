package weco.concepts.aggregator

import akka.NotUsed
import com.typesafe.config.ConfigFactory
import grizzled.slf4j.Logging
import net.ceedubs.ficus.Ficus._

import scala.concurrent.ExecutionContext
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import weco.concepts.aggregator.sources._

object Main extends App with Logging {
  val config = ConfigFactory.load()
  lazy val snapshotUrl = config.as[String]("data-source.works.snapshot")
  lazy val workUrlTemplate = config.as[String]("data-source.workURL.template")
  lazy val maxFrameKiB = config.as[Int]("data-source.maxframe.kib")
  implicit val actorSystem: ActorSystem = ActorSystem("main")
  implicit val executionContext: ExecutionContext = actorSystem.dispatcher

  // If you give it ids, it will fetch those records individually
  // If you don't it will either look at stdin or fetch the snapshot.
  val source:Source[String, NotUsed] = if (args.length > 0) WorkIdSource(args.iterator)
  else if (System.in.available() > 0) StdInSource.apply
  else WorksSnapshotSource(snapshotUrl)

  val aggregator = new ConceptsAggregator(source)
  aggregator.run
    .recover(err => error(err.getMessage))
    .onComplete(_ => actorSystem.terminate())
}
