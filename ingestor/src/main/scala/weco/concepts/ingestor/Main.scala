package weco.concepts.ingestor

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import grizzled.slf4j.Logging
import net.ceedubs.ficus.Ficus._

import scala.concurrent.ExecutionContext

object Main extends App with Logging {
  val config = ConfigFactory.load()
  val lcshUrl = config.as[String]("data-source.loc.lcsh")

  implicit val actorSystem: ActorSystem = ActorSystem("main")
  implicit val executionContext: ExecutionContext = actorSystem.dispatcher

  val ingestStream = new IngestStream(lcshUrl)
  ingestStream.run
    .recover(err => error(err.getMessage))
    .onComplete(_ => actorSystem.terminate())
}
