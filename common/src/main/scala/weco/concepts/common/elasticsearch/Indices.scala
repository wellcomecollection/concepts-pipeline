package weco.concepts.common.elasticsearch

import org.apache.pekko.Done
import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.stream.Materializer
import grizzled.slf4j.Logging
import weco.concepts.common.ResourceLoader

import scala.concurrent.Future

class Indices(val elasticHttpClient: ElasticHttpClient)(implicit
  val mat: Materializer,
  loader: ResourceLoader = ResourceLoader
) extends Creatable
    with Logging {

  def create(name: String): Future[Done] =
    store(
      uri = s"/$name",
      config = loader.loadJsonResource("index")
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
