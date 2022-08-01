package weco.concepts.aggregator

import com.typesafe.config.ConfigFactory
import grizzled.slf4j.Logging
import net.ceedubs.ficus.Ficus._

object Main extends App with Logging {
  val config = ConfigFactory.load()
  lazy val snapshotUrl = config.as[String]("data-source.works.snapshot")

  info(s"Snapshot URL: $snapshotUrl")
}
