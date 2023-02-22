package weco.concepts.common.elasticsearch

import akka.Done
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import grizzled.slf4j.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.io.{Source => IOSource}

class Scripts(elasticHttpClient: ElasticHttpClient)(implicit mat: Materializer)
    extends Logging {
  private implicit val ec: ExecutionContext = mat.executionContext

  def create(name: String, context: String): Future[Done] =
    create(
      name = name,
      context = context,
      config = IOSource.fromResource(s"$name.json").getLines().mkString("\n")
    )

  def create(name: String, context: String, config: String): Future[Done] =
    elasticHttpClient
      .singleRequest(
        HttpRequest(
          method = HttpMethods.PUT,
          uri = s"/_scripts/$name/$context",
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
            .map { errorBody =>
              throw new RuntimeException(
                s"Error when creating script $name: ${errorResponse.status} : $errorBody"
              )
            }
      }

}
