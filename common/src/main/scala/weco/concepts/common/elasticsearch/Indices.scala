package weco.concepts.common.elasticsearch

import akka.Done
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import grizzled.slf4j.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.io.{Source => IOSource}

class Indices(elasticHttpClient: ElasticHttpClient)(implicit mat: Materializer)
    extends Logging {
  private implicit val ec: ExecutionContext = mat.executionContext

  def create(name: String): Future[Done] =
    create(
      name = name,
      config = IOSource.fromResource("index.json").getLines().mkString("\n")
    )

  def create(name: String, config: String): Future[Done] =
    elasticHttpClient
      .singleRequest(
        HttpRequest(
          method = HttpMethods.PUT,
          uri = s"/$name",
          entity = HttpEntity(ContentTypes.`application/json`, config)
        )
      )
      .flatMap {
        case response if response.status.isSuccess() =>
          response.entity.discardBytes()
          Future.successful(Done)
        case errorResponse =>
          Unmarshal(errorResponse.entity)
            .to[String]
            .map {
              case body if body.contains("resource_already_exists_exception") =>
                debug(s"Index $name already exists, no need to create")
                Done
              case errorBody =>
                throw new RuntimeException(
                  s"Error when creating index $name: ${errorResponse.status} : $errorBody"
                )
            }
      }

}
