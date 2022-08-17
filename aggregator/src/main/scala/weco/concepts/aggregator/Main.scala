package weco.concepts.aggregator

import com.typesafe.config.ConfigFactory
import grizzled.slf4j.Logging
import net.ceedubs.ficus.Ficus._

import scala.concurrent.ExecutionContext
import scala.io.Source
import scala.util.{Success, Using}
import akka.actor.ActorSystem
import weco.concepts.aggregator.sources.StdInSource

object Main extends App with Logging {
  val config = ConfigFactory.load()
  lazy val snapshotUrl = config.as[String]("data-source.works.snapshot")
  lazy val workUrlTemplate = config.as[String]("data-source.workURL.template")

  if (args.length > 0) for (workId <- args) {
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
  else {
    // The differentiator for now is that if you give it some ids, it will fetch those records,
    // If you don't it will fetch the snapshot.
    info(s"Snapshot URL: $snapshotUrl")
    implicit val actorSystem: ActorSystem = ActorSystem("main")
    implicit val executionContext: ExecutionContext = actorSystem.dispatcher
//    val source = WorksSnapshotSource(snapshotUrl)
    val source = StdInSource.apply
    val aggregateStream = new AggregateStream(source)
    aggregateStream.run
      .recover(err => error(err.getMessage))
      .onComplete(_ => actorSystem.terminate())
  }
}
