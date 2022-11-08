package weco.concepts.recorder

import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import akka.{Done, NotUsed}
import akka.stream.scaladsl.Source
import grizzled.slf4j.Logging
import weco.concepts.common.elasticsearch.ElasticHttpClient
import weco.concepts.common.json.Indexable
import weco.concepts.common.json.JsonOps._

import scala.concurrent.{duration, ExecutionContext, Future}
import scala.concurrent.duration._

object IndexSource extends Logging {
  def apply[T: Indexable](
    elasticHttpClient: ElasticHttpClient,
    indexName: String,
    query: Option[ujson.Value] = None,
    keepAlive: FiniteDuration = 1 minute,
    pageSize: Int = 1000
  )(implicit mat: Materializer, ec: ExecutionContext): Source[T, NotUsed] =
    Source
      .unfoldResourceAsync[Seq[T], DeepIndexPaginator](
        create = () =>
          DeepIndexPaginator
            .init(elasticHttpClient, indexName, query, keepAlive, pageSize),
        read = _.nextPage,
        close = _.close
      )
      .statefulMapConcat(() => {
        var total = 0L
        (nextBatch: Seq[T]) => {
          total += nextBatch.size
          info(s"Total documents streamed from index: $total")
          nextBatch
        }
      })

  // https://www.elastic.co/guide/en/elasticsearch/reference/current/point-in-time-api.html
  private case class PointInTime(
    id: String,
    keepAlive: FiniteDuration
  ) {
    lazy val elasticKeepAlive: String =
      (keepAlive.length, keepAlive.unit) match {
        case (n, duration.DAYS)         => s"${n}d"
        case (n, duration.HOURS)        => s"${n}h"
        case (n, duration.MINUTES)      => s"${n}m"
        case (n, duration.SECONDS)      => s"${n}s"
        case (n, duration.MILLISECONDS) => s"${n}ms"
        case (n, duration.MICROSECONDS) => s"${n}micros"
        case (n, duration.NANOSECONDS)  => s"${n}nanos"
      }
  }

  private class DeepIndexPaginator(
    elasticHttpClient: ElasticHttpClient,
    pageSize: Int,
    var currentPit: PointInTime,
    query: Option[ujson.Value]
  )(implicit mat: Materializer, ec: ExecutionContext) {
    private var searchAfter: Option[ujson.Value] = None

    def nextPage[T: Indexable]: Future[Option[Seq[T]]] = {
      val requestBody = ujson.write(
        ujson
          .Obj(
            "size" -> pageSize,
            "query" -> query,
            "search_after" -> searchAfter,
            "sort" -> Seq(
              // https://www.elastic.co/guide/en/elasticsearch/reference/current/paginate-search-results.html
              // "Search after requests have optimizations that make them faster when the sort order is _shard_doc
              // and total hits are not tracked. If you want to iterate over all documents regardless of the order,
              // this is the most efficient option."
              ujson.Obj("_shard_doc" -> "asc")
            ),
            "pit" -> ujson.Obj(
              "id" -> currentPit.id,
              "keep_alive" -> currentPit.elasticKeepAlive
            )
          )
          .withoutNulls
      )
      elasticHttpClient
        .singleRequest(
          HttpRequest(
            method = HttpMethods.POST,
            uri = Uri("/_search"),
            entity = HttpEntity(
              ContentTypes.`application/json`,
              requestBody
            )
          )
        )
        .flatMap {
          case HttpResponse(StatusCodes.OK, _, entity, _) =>
            Unmarshal(entity).to[String].flatMap { responseBody =>
              val json = ujson.read(responseBody)
              json
                .opt[String]("pit_id")
                .map {
                  case nextPitId if nextPitId != currentPit.id =>
                    val oldPitId = currentPit.id
                    currentPit = PointInTime(nextPitId, currentPit.keepAlive)
                    deletePit(oldPitId)
                  case _ => Future.successful(Done)
                } match {
                case None =>
                  throw new RuntimeException(
                    s"Response did not contain a PIT: $responseBody"
                  )
                case Some(checkedPit) =>
                  checkedPit.map { _ =>
                    json
                      .opt[Seq[ujson.Value]]("hits", "hits")
                      .flatMap { hits =>
                        hits.lastOption.flatMap(_.opt[ujson.Value]("sort").map {
                          lastSort =>
                            searchAfter = Some(lastSort)
                            hits
                        })
                      }
                      .map { hits =>
                        for {
                          hit <- hits
                          source <- hit.opt[ujson.Value]("_source")
                          indexable <- Indexable[T].fromDoc(source)
                        } yield indexable
                      }
                  }
              }
            }
          case unexpectedResponse =>
            throw new RuntimeException(
              s"Unexpected response when searching: $unexpectedResponse"
            )
        }
    }

    def close: Future[Done] = deletePit(currentPit.id)

    private def deletePit(id: String): Future[Done] =
      elasticHttpClient
        .singleRequest(
          HttpRequest(
            method = HttpMethods.DELETE,
            uri = Uri("/_pit"),
            entity = HttpEntity(
              ContentTypes.`application/json`,
              ujson.write(ujson.Obj("id" -> id))
            )
          )
        )
        .map {
          case res if res.status.isSuccess() => Done
          case unexpectedResponse =>
            throw new RuntimeException(
              s"PIT deletion API returned unexpected response: $unexpectedResponse"
            )
        }
  }

  private object DeepIndexPaginator {
    def init(
      elasticHttpClient: ElasticHttpClient,
      indexName: String,
      query: Option[ujson.Value],
      keepAlive: FiniteDuration,
      pageSize: Int
    )(implicit
      mat: Materializer,
      ec: ExecutionContext
    ): Future[DeepIndexPaginator] = {
      getPit(elasticHttpClient, indexName, keepAlive).map(pit =>
        new DeepIndexPaginator(
          elasticHttpClient = elasticHttpClient,
          pageSize = pageSize,
          query = query,
          currentPit = pit
        )
      )
    }

    private def getPit(
      elasticHttpClient: ElasticHttpClient,
      indexName: String,
      keepAlive: FiniteDuration
    )(implicit
      mat: Materializer,
      ec: ExecutionContext
    ): Future[PointInTime] =
      elasticHttpClient
        .singleRequest(
          HttpRequest(
            method = HttpMethods.POST,
            uri = Uri(
              s"/$indexName/_pit?keep_alive=${PointInTime(id = "", keepAlive = keepAlive).elasticKeepAlive}"
            )
          )
        )
        .flatMap {
          case HttpResponse(StatusCodes.OK, _, entity, _) =>
            Unmarshal(entity).to[String].map { responseBody =>
              val json = ujson.read(responseBody)
              val pitId = json
                .opt[String]("id")
                .getOrElse(
                  throw new RuntimeException(
                    s"PIT API returned unexpected response: $responseBody"
                  )
                )
              PointInTime(
                id = pitId,
                keepAlive = keepAlive
              )
            }
          case errorResponse =>
            throw new RuntimeException(
              s"Unexpected response when initialising ES point-in-time: ${errorResponse}"
            )
        }
  }
}
