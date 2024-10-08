package weco.concepts.common.fixtures

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Flow
import org.apache.pekko.http.scaladsl.model._
import grizzled.slf4j.Logging
import weco.concepts.common.elasticsearch.ElasticHttpClient

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class TestElasticHttpClient(
  mapping: PartialFunction[HttpRequest, Future[Try[HttpResponse]]]
)(implicit ec: ExecutionContext)
    extends ElasticHttpClient
    with Logging {
  private val _requests: mutable.Buffer[HttpRequest] = mutable.Buffer.empty

  def requests: Seq[HttpRequest] = _requests.toSeq
  def lastRequest: HttpRequest = _requests.last
  def resetRequests(): Unit = {
    _requests.clear()
  }

  def flow[T]: Flow[(HttpRequest, T), (Try[HttpResponse], T), NotUsed] =
    Flow[(HttpRequest, T)]
      .map { case (request, context) =>
        _requests.append(request)
        request -> context
      }
      .mapAsync(1) { case (request, context) =>
        mapping
          .applyOrElse[HttpRequest, Future[Try[HttpResponse]]](
            request,
            unhandled =>
              throw new RuntimeException(s"Unhandled request: $unhandled")
          )
          .map(result => {
            val responseLog = result match {
              case Success(
                    HttpResponse(status, _, entity: HttpEntity.Strict, _)
                  ) =>
                s"${status.value}: ${entity.data.utf8String}"
              case Success(response)  => response.status.value
              case Failure(exception) => s"Failure: ${exception.getMessage}"
            }
            if (!isDebugEnabled) {
              info(s"${request.method.value}: ${request.uri.toString()}")
            }
            debug(
              s"${request.method.value}: ${request.uri.toString()} -> $responseLog"
            )
            result -> context
          })
      }
}

object TestElasticHttpClient {
  def apply(mapping: PartialFunction[HttpRequest, Try[HttpResponse]])(implicit
    ec: ExecutionContext
  ) =
    new TestElasticHttpClient(mapping.andThen(Future.successful(_)))
}
