package weco.concepts.aggregator

import com.typesafe.config.{Config, ConfigFactory}
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import akka.actor.ActorSystem
import grizzled.slf4j.Logging
import weco.concepts.aggregator.secrets.{
  ClusterConfWithSecrets,
  SecretsResolver
}
import weco.concepts.aggregator.sources.WorkIdSource
import weco.concepts.common.elasticsearch.Indexer

import scala.concurrent.ExecutionContext

/** Common base for the entrypoint to aggregator, regardless of how it is called
  * This loads the appropriate configuration for the application, chosen by the
  * AGGREGATOR_APP_CONTEXT environment variable. If absent, the application will
  * run as though Elasticsearch is running on the same host.
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
  private val config: Config = {
    val configName = sys.env.getOrElse("AGGREGATOR_APP_CONTEXT", "local")
    info(s"loading config $configName")
    ConfigFactory.load(configName)
  }

  protected val maxFrameKiB: Int =
    config.as[Int]("data-source.maxframe.kib")

  protected lazy val workIdSource: WorkIdSource = WorkIdSource(
    config.as[String]("data-source.workURL.template")
  )
  protected lazy val snapshotUrl: String =
    config.as[String]("data-source.works.snapshot")

  private val clusterConf = new ClusterConfWithSecrets(
    SecretsResolver(config.as[String]("secrets-resolver"))
  )(config.as[Indexer.ClusterConfig]("data-target.cluster"))

  protected val indexer: Indexer = Indexer(
    clusterConf
  )

  implicit val actorSystem: ActorSystem = ActorSystem("main")
  implicit val executionContext: ExecutionContext =
    actorSystem.dispatcher

  val aggregator: ConceptsAggregator = new ConceptsAggregator(
    indexer = indexer,
    indexName = config.as[String]("data-target.index.name"),
    maxRecordsPerBulkRequest = config.as[Int]("data-target.bulk.max-records")
  )

}
