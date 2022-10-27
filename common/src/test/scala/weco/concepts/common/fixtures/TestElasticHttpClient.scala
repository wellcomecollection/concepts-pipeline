package weco.concepts.common.fixtures

import akka.NotUsed
import akka.stream.scaladsl.Flow
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import weco.concepts.common.elasticsearch.ElasticHttpClient

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

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

  def defaultBulkHandler(implicit
    mat: Materializer
  ): PartialFunction[HttpRequest, Future[Try[HttpResponse]]] = {
    case HttpRequest(HttpMethods.POST, uri, _, entity, _)
        if uri.path.toString() == "/_bulk" =>
      implicit val ec: ExecutionContext = mat.executionContext
      Unmarshal(entity).to[String].map { bulkUpdateRequest =>
        val couplets = bulkUpdateRequest
          .split('\n')
          .map(ujson.read(_))
          .grouped(2)
          .collect { case Array(a, b) => a -> b }
          .toSeq
        val result = ujson.Obj(
          "took" -> 1234,
          "errors" -> false,
          "items" -> couplets.map { case (action, _) =>
            ujson.Obj(
              "update" -> ujson.Obj(
                "_id" -> action("update")("_id").str,
                "result" -> "created"
              )
            )
          }
        )
        Success(
          HttpResponse(
            StatusCodes.OK,
            entity = HttpEntity(
              ContentTypes.`application/json`,
              ujson.write(result)
            )
          )
        )
      }
  }
}
