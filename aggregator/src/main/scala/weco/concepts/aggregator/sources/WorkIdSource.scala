package weco.concepts.aggregator.sources

import scala.util.{Failure, Success, Try}
import akka.stream.scaladsl.{Flow, Source}
import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import grizzled.slf4j.Logging

/** Fetch works with given workIds from the Catalogue API
  */
class WorkIdSource(
  workUrlTemplate: String,
  httpFlow: Flow[(HttpRequest, String), (Try[HttpResponse], String), NotUsed]
)(implicit actorSystem: ActorSystem)
    extends Logging {

  def apply(workIds: Iterator[String]): Source[String, NotUsed] = {
    info(s"reading from catalogue API")
    if (!workUrlTemplate.contains("api.wellcomecollection.org")) {
      // If you see this warning and you do not expect it (e.g. in a production system),
      // check the workurl_template environment variable.
      // This environment variable should normally be unset in production,
      // allowing the default template from the config files to be used.
      warn(
        s"Reading from non-standard catalogue API - template: $workUrlTemplate"
      )
    }
    Source
      .fromIterator(() => workIds.distinct)
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
      .via(httpFlow)
      .map {
        case (Success(HttpResponse(StatusCodes.OK, _, entity, _)), _) =>
          Some(entity)
        case (Success(HttpResponse(StatusCodes.Gone, _, _, _)), goneWorkId) =>
          // This does mean that we don't handle the case that a deleted/suppressed work removes
          // a concept from use in the catalogue (resulting in a concept page being "orphaned").
          // In reality, we don't mind this, and deletions are more likely to be handled by
          // whole-catalogue runs of the aggregator over the snapshots.
          info(s"Updated work was removed from API: $goneWorkId")
          None
        case (
              Success(HttpResponse(StatusCodes.Found, headers, _, _)),
              redirectedWorkId
            ) =>
          // Redirected works normally point to a different substantive work.
          // That will be where the relevant changes can be found.
          // If there are relevant changes, then the target of the redirection will also
          // have changed and will be in the queue to be processed.
          // Therefore, there is no point in following the redirection, as the target
          // is either about to be, or recently has been processed in its own right.
          val redirectedTo: String =
            headers.find(_.name() == "Location") match {
              case Some(location) => location.value
              case None           => "nowhere"
            }
          info(
            s"Work is redirected, not fetching: $redirectedWorkId, redirected to: $redirectedTo"
          )
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
