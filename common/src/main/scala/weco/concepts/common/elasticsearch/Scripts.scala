package weco.concepts.common.elasticsearch

import org.apache.pekko.Done
import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.stream.Materializer
import grizzled.slf4j.Logging
import weco.concepts.common.ResourceLoader

import scala.concurrent.Future

class Scripts(val elasticHttpClient: ElasticHttpClient)(implicit
  val mat: Materializer,
  loader: ResourceLoader = ResourceLoader
) extends Creatable
    with Logging {

  def create(
    name: String,
    context: String
  ): Future[Done] =
    store(
      uri = s"/_scripts/$name/$context",
      config = loader.loadJsonResource(name)
    )
  override protected def interpretErrorResponse(
    uri: String,
    errorResponse: HttpResponse,
    errorBody: String
  ): Done = throw new RuntimeException(
    s"Error when creating script $uri: ${errorResponse.status} : $errorBody"
  )
}
