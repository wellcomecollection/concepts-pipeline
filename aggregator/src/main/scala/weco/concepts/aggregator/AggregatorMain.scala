package weco.concepts.aggregator

import akka.NotUsed
import com.typesafe.config.{Config, ConfigFactory}
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import weco.concepts.aggregator.sources.{WorkIdSource, WorksSnapshotSource}

import scala.concurrent.ExecutionContext

/** Common base for the entrypoint to aggregator, regardless of how it is called
  * This loads the appropriate configuration for the application, chosen by the
  * AGGREGATOR_APP_CONTEXT environment variable. If absent, the application will
  * run as though Elasticsearch is running on the same host.
  *
  * Implementations inheriting from this trait are expected to determine a
  * source of Works JSON from their calling parameters and run the
  * ConceptAggregator with it.
  */
trait AggregatorMain extends ClusterEnvConfig {
  protected implicit val actorSystem: ActorSystem = ActorSystem("main")
  protected implicit val executionContext: ExecutionContext =
    actorSystem.dispatcher
  private val config: Config =
    ConfigFactory.load(
      sys.env.getOrElse("AGGREGATOR_APP_CONTEXT", "local")
    )

  private lazy val workUrlTemplate =
    config.as[String]("data-source.workURL.template")

  protected val resolveClusterSecrets: Boolean =
    config.as[Boolean]("data-target.resolve-secrets")
  protected val indexer: Indexer = getIndexer(
    config.as[Indexer.ClusterConfig]("data-target.cluster")
  )
  private val indexName = config.as[String]("data-target.index.name")
  private val maxBulkRecords =
    config.as[Int]("data-target.bulk.max-records")

  private lazy val snapshotUrl = config.as[String]("data-source.works.snapshot")
  protected lazy val maxFrameKiB: Int =
    config.as[Int]("data-source.maxframe.kib")

  protected lazy val workIdSource: WorkIdSource = WorkIdSource(workUrlTemplate)
  protected lazy val snapshotSource: Source[String, NotUsed] =
    WorksSnapshotSource(maxFrameKiB, snapshotUrl)

  protected val aggregator: ConceptsAggregator = new ConceptsAggregator(
    indexer = indexer,
    indexName = indexName,
    maxRecordsPerBulkRequest = maxBulkRecords
  )

}
