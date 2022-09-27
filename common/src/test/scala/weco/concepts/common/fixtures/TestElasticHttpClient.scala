package weco.concepts.common.fixtures

import akka.NotUsed
import akka.stream.scaladsl.Flow
import akka.http.scaladsl.model._
import weco.concepts.common.elasticsearch.ElasticHttpClient

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class TestElasticHttpClient(
  mapping: PartialFunction[HttpRequest, Future[Try[HttpResponse]]]
)(implicit ec: ExecutionContext)
    extends ElasticHttpClient {
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
          .map(_ -> context)
      }
}

object TestElasticHttpClient {
  def apply(mapping: PartialFunction[HttpRequest, Try[HttpResponse]])(implicit
    ec: ExecutionContext
  ) =
    new TestElasticHttpClient(mapping.andThen(Future.successful(_)))
}
