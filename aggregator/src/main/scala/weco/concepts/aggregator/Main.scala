package weco.concepts.aggregator

import com.typesafe.config.ConfigFactory
import grizzled.slf4j.Logging
import net.ceedubs.ficus.Ficus._

import scala.io.Source
import scala.util.{Success, Using}

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
    info(s"Snapshot URL: $snapshotUrl")
  }
}
