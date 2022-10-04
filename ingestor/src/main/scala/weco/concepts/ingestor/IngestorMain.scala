package weco.concepts.ingestor

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import grizzled.slf4j.Logging
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import weco.concepts.common.elasticsearch.ElasticAkkaHttpClient
import weco.concepts.common.secrets.{ClusterConfWithSecrets, SecretsResolver}

import scala.concurrent.ExecutionContext

trait IngestorMain extends Logging {
  private val config: Config = {
    val configName = sys.env.getOrElse("INGESTOR_APP_CONTEXT", "local")
    info(s"loading config $configName")
    ConfigFactory.load(configName)
  }
  val lcshUrl = config.as[String]("data-source.loc.lcsh")
  val lcNamesUrl = config.as[String]("data-source.loc.names")
  val indexName = config.as[String]("data-target.index.name")
  val maxBulkRecords = config.as[Int]("data-target.bulk.max-records")

  implicit val actorSystem: ActorSystem = ActorSystem("main")
  implicit val executionContext: ExecutionContext = actorSystem.dispatcher

  private val clusterConfig = new ClusterConfWithSecrets(
    SecretsResolver(config.as[String]("secrets-resolver"))
  )(config.as[ElasticAkkaHttpClient.ClusterConfig]("data-target.cluster"))

  val elasticHttpClient = ElasticAkkaHttpClient(clusterConfig)
  val ingestStream = new IngestStream(
    subjectsUrl = lcshUrl,
    namesUrl = lcNamesUrl,
    elasticHttpClient = elasticHttpClient,
    indexName = indexName,
    maxRecordsPerBulkRequest = maxBulkRecords
  )

}
