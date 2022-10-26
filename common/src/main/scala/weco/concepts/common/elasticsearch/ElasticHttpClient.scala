package weco.concepts.common.elasticsearch

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import akka.stream.scaladsl.Flow

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Try

trait ElasticHttpClient {
  def flow[T]: Flow[(HttpRequest, T), (Try[HttpResponse], T), NotUsed]
}

class ElasticAkkaHttpClient(
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

object ElasticAkkaHttpClient {
  case class ClusterConfig(
    scheme: String,
    host: String,
    port: Int,
    username: Option[String],
    password: Option[String],
    resolveSecrets: Boolean
  )

  def apply(clusterConfig: ClusterConfig)(implicit actorSystem: ActorSystem) =
    new ElasticAkkaHttpClient(
      scheme = clusterConfig.scheme,
      host = clusterConfig.host,
      port = clusterConfig.port,
      username = clusterConfig.username,
      password = clusterConfig.password
    )

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
