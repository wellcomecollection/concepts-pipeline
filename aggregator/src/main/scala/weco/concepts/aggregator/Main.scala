package weco.concepts.aggregator

import com.typesafe.config.ConfigFactory
import grizzled.slf4j.Logging
import net.ceedubs.ficus.Ficus._

import scala.concurrent.ExecutionContext
import scala.io.Source
import scala.util.{Success, Using}
import akka.actor.ActorSystem
import weco.concepts.aggregator.sources.{StdInSource, WorksSnapshotSource}

object Main extends App with Logging {
  val config = ConfigFactory.load()
  lazy val snapshotUrl = config.as[String]("data-source.works.snapshot")
  lazy val workUrlTemplate = config.as[String]("data-source.workURL.template")
  lazy val maxFrameKiB = config.as[Int]("data-source.maxframe.kib")

  // If you give it ids, it will fetch those records individually
  // If you don't it will either look at stdin or fetch the snapshot.
  if (args.length > 0) extractByIds(args)
  else extractFromNDJSon()

  private def extractByIds(workIds:Array[String]): Unit = {
    for (workId <- workIds) {
      // for now, just fetch it from the API.  Once we start deploying it for use,
      // and it has access to databases, it may be better to pull it out from there
      // instead to avoid load on the API.
      info(workId)
      val concepts = Using(Source.fromURL(workUrlTemplate.format(workId))) {
        source => source.mkString
      } match {
        case Success(jsonString) =>
          ConceptExtractor(jsonString)
        case _ => Nil
      }
      info(concepts)
    }
  }

  private def extractFromNDJSon(): Unit = {
    info(s"Snapshot URL: $snapshotUrl")
    implicit val actorSystem: ActorSystem = ActorSystem("main")
    implicit val executionContext: ExecutionContext = actorSystem.dispatcher
    val maxFrameBytes = maxFrameKiB * 1024
    val source =
      if (System.in.available() > 0) StdInSource(maxFrameBytes)
      else WorksSnapshotSource(snapshotUrl, maxFrameBytes)
    val aggregateStream = new AggregateStream(source)
    aggregateStream.run
      .recover(err => error(err.getMessage))
      .onComplete(_ => actorSystem.terminate())
  }

}
