package weco.concepts.common.fixtures

import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import weco.concepts.common.json.Indexable
import weco.concepts.common.json.Indexable._
import weco.concepts.common.json.JsonOps._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Random, Success, Try}

object ElasticsearchResponses {
  def handleBulkUpdate(implicit
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

  def handlePitCreation
    : PartialFunction[HttpRequest, Future[Try[HttpResponse]]] = {
    case HttpRequest(HttpMethods.POST, uri, _, _, _)
        if uri.path.endsWith("_pit") =>
      Future.successful(
        Success(
          HttpResponse(
            status = StatusCodes.OK,
            entity = HttpEntity(
              ContentTypes.`application/json`,
              ujson.write(
                ujson.Obj(
                  "id" -> Random.alphanumeric.take(32).mkString
                )
              )
            )
          )
        )
      )
  }

  def handlePitDeletion
    : PartialFunction[HttpRequest, Future[Try[HttpResponse]]] = {
    case HttpRequest(HttpMethods.DELETE, uri, _, _, _)
        if uri.path.endsWith("_pit") =>
      Future.successful(
        Success(
          HttpResponse(
            status = StatusCodes.OK,
            entity = HttpEntity(
              ContentTypes.`application/json`,
              ujson.write(
                ujson.Obj(
                  "succeeded" -> true,
                  "num_freed" -> 1
                )
              )
            )
          )
        )
      )
  }

  def handleSearch[T: Indexable](
    docs: Seq[T],
    transformPit: String => String = identity
  )(implicit
    mat: Materializer
  ): PartialFunction[HttpRequest, Future[Try[HttpResponse]]] = {
    case HttpRequest(HttpMethods.POST, uri, _, entity, _)
        if uri == Uri("/_search") =>
      implicit val ec: ExecutionContext = mat.executionContext
      Unmarshal(entity).to[String].map(ujson.read(_)).map { requestBody =>
        val pit = transformPit(requestBody("pit")("id").str)
        val searchAfter =
          requestBody
            .opt[Seq[Int]]("search_after")
            .flatMap(_.headOption)
            .map(_ + 1) // Because we're slicing using the indices below
            .getOrElse(0)
        val size = requestBody("size").num.toInt
        Success(
          HttpResponse(
            status = StatusCodes.OK,
            entity = HttpEntity(
              ContentTypes.`application/json`,
              ujson.write(
                ujson.Obj(
                  "pit_id" -> pit,
                  "hits" -> ujson.Obj(
                    "hits" ->
                      docs.zipWithIndex
                        .slice(
                          searchAfter,
                          searchAfter + size
                        )
                        .map { case (doc, idx) =>
                          ujson.Obj(
                            "_source" -> doc.toDoc,
                            "sort" -> Seq(idx)
                          )
                        }
                  )
                )
              )
            )
          )
        )
      }
  }

  def mgetResponse[T: Indexable](
    index: String,
    existing: Seq[T],
    entity: HttpEntity
  )(implicit mat: Materializer): Future[Try[HttpResponse]] = {
    implicit val ec: ExecutionContext = mat.executionContext
    Unmarshal(entity).to[String].map(ujson.read(_)).map { requestBody =>
      val requestedIds = requestBody("ids").arr.map(_.str)
      val existingMap = existing.map(doc => doc.id -> doc).toMap
      val docs = requestedIds
        .map(id =>
          existingMap
            .get(id)
            .map(indexable =>
              ujson.Obj(
                "_index" -> index,
                "_id" -> id,
                "_version" -> 1,
                "found" -> true,
                "_source" -> indexable.toDoc
              )
            )
            .getOrElse(
              ujson.Obj("_index" -> index, "_id" -> id, "found" -> false)
            )
        )
      Success(
        HttpResponse(
          StatusCodes.OK,
          entity = HttpEntity(
            ContentTypes.`application/json`,
            ujson.write(ujson.Obj("docs" -> docs))
          )
        )
      )
    }
  }
}
