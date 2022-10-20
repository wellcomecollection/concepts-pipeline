package weco.concepts.aggregator.sources

import scala.util.{Failure, Success}
import akka.stream.scaladsl.{Flow, Source}
import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import grizzled.slf4j.Logging

/** Fetch works with given workIds from the Catalogue API
  */
class WorkIdSource(workUrlTemplate: String)(implicit actorSystem: ActorSystem)
    extends Logging {
  private lazy val pool = Http().superPool[String]()

  def apply(workIds: Seq[String]): Source[String, NotUsed] = {
    info(s"reading from catalogue API")
    Source(workIds)
      .via(jsonFromWorkId)
  }

  private def jsonFromWorkId: Flow[String, String, NotUsed] =
    Flow[String]
      .map { workId =>
        HttpRequest(
          method = HttpMethods.GET,
          uri = Uri(workUrlTemplate.format(workId))
        ) -> workId
      }
      .via(pool)
      .map {
        case (Success(HttpResponse(StatusCodes.OK, _, entity, _)), _) =>
          Some(entity)
        case (Success(HttpResponse(StatusCodes.Gone, _, _, _)), goneWorkId) =>
          info(s"Updated work was removed from API: $goneWorkId")
          None
        case (Success(errorResponse), failedWorkId) =>
          warn(
            s"Could not fetch $failedWorkId: request returned ${errorResponse.status.value}"
          )
          None
        case (Failure(_), failedWorkId) =>
          throw new RuntimeException(
            s"Failure connecting to Catalogue API when fetching $failedWorkId"
          )
      }
      .collect { case Some(entity) => entity }
      .mapAsyncUnordered(10) { Unmarshal(_).to[String] }
}
