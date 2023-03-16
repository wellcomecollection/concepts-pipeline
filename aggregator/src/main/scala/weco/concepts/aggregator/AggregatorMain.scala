package weco.concepts.aggregator

import akka.Done
import com.typesafe.config.{Config, ConfigFactory}
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import grizzled.slf4j.Logging
import weco.concepts.aggregator.sources.WorkIdSource
import weco.concepts.common.elasticsearch.{
  ElasticAkkaHttpClient,
  ElasticHttpClient
}
import weco.concepts.common.secrets.{ClusterConfWithSecrets, SecretsResolver}

import scala.concurrent.{ExecutionContext, Future}

/** Common base for the entrypoint to aggregator, regardless of how it is called
  * This loads the appropriate configuration for the application, chosen by the
  * APP_CONTEXT environment variable. If absent, the application will run as
  * though Elasticsearch is running on the same host.
  *
  * Implementations inheriting from this trait are expected to determine a
  * source of Works JSON from their calling parameters and run the
  * ConceptAggregator with it.
  *
  * One of the implementations that inherits from this trait is a Lambda
  * function, Because of this, as much common setup as possible is done in the
  * construction so that the only work that needs to be done on a per-call basis
  * is that which differs per-call.
  */
trait AggregatorMain extends Logging {
  protected val config: Config = {
    val configName = sys.env.getOrElse("APP_CONTEXT", "local")
    info(s"loading config $configName")
    ConfigFactory.load(configName)
  }

  protected val maxFrameKiB: Int =
    config.as[Int]("data-source.maxframe.kib")

  protected lazy val workIdSource: WorkIdSource = new WorkIdSource(
    config.as[String]("data-source.workURL.template")
  )
  protected lazy val snapshotUrl: String =
    config.as[String]("data-source.works.snapshot")

  implicit val actorSystem: ActorSystem = ActorSystem("main")
  implicit val executionContext: ExecutionContext = actorSystem.dispatcher

  private val clusterConfig = new ClusterConfWithSecrets(
    SecretsResolver(config.as[String]("secrets-resolver"))
  )(config.as[ElasticAkkaHttpClient.ClusterConfig]("data-target.cluster"))

  private val elasticHttpClient: ElasticHttpClient = ElasticAkkaHttpClient(
    clusterConfig
  )

  protected val updatesSink: Sink[String, Future[Done]]

  lazy val aggregator: ConceptsAggregator = new ConceptsAggregator(
    elasticHttpClient = elasticHttpClient,
    updatesSink = updatesSink,
    indexName = config.as[String]("data-target.index.name"),
    maxRecordsPerBulkRequest = config.as[Int]("data-target.bulk.max-records"),
    shouldUpdateAppenderScript =
      config.as[Boolean]("data-target.appenderScript.updateAtRuntime")
  )
}
