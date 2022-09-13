package weco.concepts.aggregator.sources

import scala.util.{Failure, Success, Using}
import scala.io.{Source => IoSource}
import akka.stream.scaladsl.{Flow, Source => AkkaSource}
import akka.NotUsed
import grizzled.slf4j.Logging

/** Fetch works with given workIds from the Catalogue API
  */
class WorkIdSource(workUrlTemplate: String) extends Logging {
  def apply(workIds: Iterator[String]): AkkaSource[String, NotUsed] = {
    info(s"reading from catalogue API")
    AkkaSource
      .fromIterator(() => workIds.iterator)
      .via(
        Flow.fromFunction(JSonFromWorkId)
      )
      .mapConcat(identity)
  }

  private def JSonFromWorkId(workId: String): Option[String] = {
    val workUrl = workUrlTemplate.format(workId)
    info(s"fetching $workUrl")
    // TODO: fromURL won't time out, so if the catalogue API is
    //   unresponsive (not immediately returning errors), then
    //   each call could take "forever", hitting the timeout for the application
    //   It may be a good idea to add a very short timeout here so that it
    //   can fail faster and possibly in a more informative manner.
    // TODO: Also, consider setting this up to maintain the connection between calls,
    //   That way subsequent Lambdas will be faster.
    Using(IoSource.fromURL(workUrlTemplate.format(workId))) { source =>
      source.mkString
    } match {
      case Success(jsonString) =>
        debug(s"fetched data for $workId")
        Some(jsonString)
      case Failure(exception) =>
        error(s"could not fetch $workId, $exception")
        None
    }
  }
}

object WorkIdSource {
  def apply(workUrlTemplate: String) = new WorkIdSource(workUrlTemplate)
}
