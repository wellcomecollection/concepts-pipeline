package weco.concepts.ingestor

import com.typesafe.config.ConfigFactory
import grizzled.slf4j.Logging
import net.ceedubs.ficus.Ficus._

object Main extends App with Logging {
  val config = ConfigFactory.load()
  val lcshUrl = config.as[String]("data-source.loc.lcsh")

  info(s"LCSH URL: $lcshUrl")
}
