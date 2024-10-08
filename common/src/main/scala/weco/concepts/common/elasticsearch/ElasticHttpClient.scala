package weco.concepts.common.elasticsearch

import org.apache.pekko.NotUsed
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model.{HttpRequest, HttpResponse}
import org.apache.pekko.http.scaladsl.model.headers.BasicHttpCredentials
import org.apache.pekko.http.scaladsl.settings.ConnectionPoolSettings
import org.apache.pekko.http.scaladsl.unmarshalling.Unmarshal
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.Try

trait ElasticHttpClient {
  def flow[T]: Flow[(HttpRequest, T), (Try[HttpResponse], T), NotUsed]
  def singleRequest(
    request: HttpRequest
  )(implicit mat: Materializer): Future[HttpResponse] =
    Source
      .single(request -> request)
      .via(flow[HttpRequest])
      .collect {
        case (result, matchingRequest) if matchingRequest == request => result
      }
      .mapAsyncUnordered(10)(Future.fromTry)
      .runWith(Sink.head)
}

class ElasticPekkoHttpClient(
  scheme: String,
  host: String,
  port: Int,
  username: Option[String],
  password: Option[String]
)(implicit actorSystem: ActorSystem)
    extends ElasticHttpClient {
  def flow[T]: Flow[(HttpRequest, T), (Try[HttpResponse], T), NotUsed] =
    addCredentialsFlow[T]().via(scheme match {
      case "https" =>
        Http()
          .cachedHostConnectionPoolHttps(host, port, settings = poolSettings)
      case _ =>
        Http().cachedHostConnectionPool(host, port, settings = poolSettings)
    })

  private lazy val poolSettings = ConnectionPoolSettings(actorSystem)
    .withKeepAliveTimeout(50 seconds) // Less than an ELB default of 60s

  private def addCredentialsFlow[T]() = (username, password) match {
    case (Some(user), Some(pass)) =>
      Flow[(HttpRequest, T)].map { case (req, context) =>
        (req.addCredentials(BasicHttpCredentials(user, pass)), context)
      }
    case _ => Flow[(HttpRequest, T)]
  }
}

object ElasticPekkoHttpClient {
  case class ClusterConfig(
    scheme: String,
    host: String,
    port: Int,
    username: Option[String],
    password: Option[String],
    resolveSecrets: Boolean
  )

  def apply(clusterConfig: ClusterConfig)(implicit actorSystem: ActorSystem) =
    new ElasticPekkoHttpClient(
      scheme = clusterConfig.scheme,
      host = clusterConfig.host,
      port = clusterConfig.port,
      username = clusterConfig.username,
      password = clusterConfig.password
    )

  // This isn't part of the main client as we may wish to stream responses or handle statuses
  // differently - the parallelism is rather arbitrary as in a non-streaming context
  // it's a CPU-bound operation.
  def deserializeJson: Flow[HttpResponse, ujson.Value, NotUsed] =
    Flow
      .fromMaterializer { (materializer, _) =>
        implicit val mat: Materializer = materializer
        implicit val ec: ExecutionContext = mat.executionContext
        Flow[HttpResponse]
          .mapAsyncUnordered(10)(Unmarshal(_).to[String])
          .map(ujson.read(_))
      }
      .mapMaterializedValue(_ => NotUsed)
}
