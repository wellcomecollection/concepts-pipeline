package weco.concepts.aggregator.sources

import scala.util.{Failure, Success, Using}
import scala.io.{Source => IoSource}

import akka.stream.scaladsl.{Flow, Source => AkkaSource}
import akka.NotUsed
import grizzled.slf4j.Logging

import weco.concepts.aggregator.Main.workUrlTemplate

/**
 * Fetch works with given workIds from the Catalogue API
 */
object WorkIdSource extends Logging {
  def apply(workIds: Iterator[String]): AkkaSource[String, NotUsed] = {
    AkkaSource
      .fromIterator(() => workIds.iterator)
      .via(
        Flow.fromFunction(JSonFromWorkId)
      )
      .mapConcat(identity)
  }

  private def JSonFromWorkId(workId: String): Option[String] = {
    Using(IoSource.fromURL(workUrlTemplate.format(workId))) { source =>
      source.mkString
    } match {
      case Success(jsonString) =>
        Some(jsonString)
      case Failure(exception) =>
        error(s"could not fetch $workId, $exception")
        None
    }
  }
}
