package weco.concepts.recorder

import org.apache.pekko.NotUsed
import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.stream.scaladsl.Flow
import grizzled.slf4j.Logging
import weco.concepts.common.elasticsearch.{
  ElasticPekkoHttpClient,
  ElasticHttpClient
}
import weco.concepts.common.json.Indexable
import weco.concepts.common.json.JsonOps._

import scala.util.{Failure, Success}

class MultiGetFlow(
  elasticHttpClient: ElasticHttpClient,
  maxBatchSize: Int = 1000
) extends Logging {
  def forIndex[T: Indexable](
    indexName: String
  ): Flow[String, Option[T], NotUsed] =
    Flow[String]
      .grouped(maxBatchSize)
      .map { ids =>
        val mgetBody = ujson.Obj("ids" -> ids).render()
        HttpRequest(
          method = HttpMethods.GET,
          uri = Uri(s"/$indexName/_mget"),
          entity = HttpEntity(ContentTypes.`application/json`, mgetBody)
        ) -> ()
      }
      .via(elasticHttpClient.flow[Unit])
      .map {
        case (Success(response), _) if response.status.isSuccess() =>
          response
        case (Success(errorResponse), _) =>
          error("Error response returned when getting docs")
          throw new RuntimeException(s"Response: $errorResponse")
        case (Failure(exception), _) =>
          error("Unexpected error performing multi-get")
          throw exception
      }
      .via(ElasticPekkoHttpClient.deserializeJson)
      .mapConcat(
        _.opt[Seq[ujson.Value]]("docs")
          .map(_.map {
            case doc if doc.opt[Boolean]("found").contains(false) => None
            case doc =>
              doc.opt[ujson.Value]("_source").flatMap(Indexable[T].fromDoc(_))
          })
          .getOrElse(Nil)
      )
}
