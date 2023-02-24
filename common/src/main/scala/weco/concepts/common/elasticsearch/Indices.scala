package weco.concepts.common.elasticsearch

import akka.Done
import akka.http.scaladsl.model._
import akka.stream.Materializer
import grizzled.slf4j.Logging

import scala.concurrent.Future

class Indices(val elasticHttpClient: ElasticHttpClient)(implicit
  val mat: Materializer,
  loader: ResourceLoader = ResourceFileLoader
) extends Creatable
    with Logging {

  def create(name: String): Future[Done] =
    store(
      uri = s"/$name",
      config = loader.loadJsonResource(name)
    )

  override protected def interpretErrorResponse(
    uri: String,
    errorResponse: HttpResponse,
    errorBody: String
  ): Done =
    errorBody match {
      case body if body.contains("resource_already_exists_exception") =>
        debug(s"Index $$name already exists, no need to create")
        Done
      case errorBody =>
        throw new RuntimeException(
          s"Error when creating index $uri: ${errorResponse.status} : $errorBody"
        )
    }
}
