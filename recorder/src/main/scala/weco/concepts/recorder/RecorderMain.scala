package weco.concepts.recorder

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import grizzled.slf4j.Logging
import weco.concepts.common.elasticsearch.{
  ElasticAkkaHttpClient,
  ElasticHttpClient,
  Indices
}
import weco.concepts.common.secrets.{ClusterConfWithSecrets, SecretsResolver}

import scala.concurrent.ExecutionContext

trait RecorderMain extends Logging {
  private val config: Config = {
    val configName = sys.env.getOrElse("APP_CONTEXT", "local")
    info(s"loading config $configName")
    ConfigFactory.load(configName)
  }
  implicit val actorSystem: ActorSystem = ActorSystem("main")
  implicit val executionContext: ExecutionContext = actorSystem.dispatcher

  private val clusterConfig = new ClusterConfWithSecrets(
    SecretsResolver(config.as[String]("secrets-resolver"))
  )(config.as[ElasticAkkaHttpClient.ClusterConfig]("data.cluster"))

  private val elasticHttpClient: ElasticHttpClient = ElasticAkkaHttpClient(
    clusterConfig
  )
  val indices: Indices = new Indices(elasticHttpClient)
  val targetIndex: String = config.as[String]("data-target.index.name")

  val recorderStream: RecorderStream = new RecorderStream(
    elasticHttpClient = elasticHttpClient,
    authoritativeConceptsIndexName =
      config.as[String]("data-source.index.authoritative.name"),
    catalogueConceptsIndexName =
      config.as[String]("data-source.index.catalogue.name"),
    targetIndexName = targetIndex
  )
}
