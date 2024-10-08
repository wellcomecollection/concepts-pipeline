package weco.concepts.common.elasticsearch

import org.apache.pekko.Done
import org.apache.pekko.http.scaladsl.model.{
  ContentTypes,
  HttpEntity,
  HttpMethods,
  HttpRequest,
  HttpResponse
}
import org.apache.pekko.http.scaladsl.unmarshalling.Unmarshal
import org.apache.pekko.stream.Materializer

import scala.concurrent.{ExecutionContext, Future}

/** Common behaviour for resources that are creatable in Elasticsearch, such as
  * indices, templates and scripts.
  */
trait Creatable {
  protected implicit val mat: Materializer
  private implicit val ec: ExecutionContext = mat.executionContext
  protected val elasticHttpClient: ElasticHttpClient

  /** Respond appropriately if Elasticsearch returns a failure response.
    *
    * In some cases, a failure may be expected and discarded, e.g. when we want
    * to be idempotent, but ElasticSearch abhors duplicates.
    *
    * In others, we may wish to throw a helpful exception.
    */

  protected def interpretErrorResponse(
    uri: String,
    errorResponse: HttpResponse,
    errorBody: String
  ): Done

  def store(uri: String, config: String): Future[Done] =
    elasticHttpClient
      .singleRequest(
        HttpRequest(
          method = HttpMethods.PUT,
          uri = uri,
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
            .map(interpretErrorResponse(uri, errorResponse, _))
      }

}
