package weco.concepts.ingestor

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import weco.concepts.common.elasticsearch.ElasticAkkaHttpClient

import scala.concurrent.ExecutionContext

trait IngestorMain {
  val config = ConfigFactory.load()
  val lcshUrl = config.as[String]("data-source.loc.lcsh")
  val lcNamesUrl = config.as[String]("data-source.loc.names")
  val indexName = config.as[String]("data-target.index.name")
  val maxBulkRecords = config.as[Int]("data-target.bulk.max-records")
  val clusterConfig =
    config.as[ElasticAkkaHttpClient.ClusterConfig]("data-target.cluster")

  implicit val actorSystem: ActorSystem = ActorSystem("main")
  implicit val executionContext: ExecutionContext = actorSystem.dispatcher

  val elasticHttpClient = ElasticAkkaHttpClient(clusterConfig)
  val ingestStream = new IngestStream(
    subjectsUrl = lcshUrl,
    namesUrl = lcNamesUrl,
    elasticHttpClient = elasticHttpClient,
    indexName = indexName,
    maxRecordsPerBulkRequest = maxBulkRecords
  )

}
