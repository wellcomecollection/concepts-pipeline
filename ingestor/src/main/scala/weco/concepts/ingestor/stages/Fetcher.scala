package weco.concepts.ingestor.stages

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.scaladsl._
import akka.util.ByteString
import grizzled.slf4j.Logging

import scala.util.{Failure, Success}

class Fetcher(implicit actorSystem: ActorSystem) extends Logging {
  private lazy val httpFlow = Http().superPool[String]()

  def fetchFromUrl(url: String): Source[ByteString, NotUsed] =
    Source
      .single(HttpRequest(method = HttpMethods.GET, uri = Uri(url)) -> url)
      .via(httpFlow)
      .flatMapConcat {
        case (Success(HttpResponse(StatusCodes.OK, _, entity, _)), _) =>
          info(
            s"Started streaming response from $url (${describeContentLength(entity)})"
          )
          entity.dataBytes
        case (Success(response), _) if response.status.isRedirection() =>
          response.header[headers.Location].map(_.uri.toString()) match {
            case Some(location) =>
              info(s"Received redirect from $url to $location")
              fetchFromUrl(location)
            case None =>
              throw new RuntimeException(
                s"Received a redirect response without a Location header from $url"
              )
          }
        case (Success(response), _) =>
          throw new RuntimeException(
            s"Unexpected status ${response.status.value} from $url"
          )
        case (Failure(exception), _) =>
          throw new RuntimeException(s"Failure making request: $exception")
      }

  private def describeContentLength(entity: ResponseEntity): String =
    entity.contentLengthOption
      .map(length => s"${length.toString} bytes")
      .getOrElse("unknown size")
}
