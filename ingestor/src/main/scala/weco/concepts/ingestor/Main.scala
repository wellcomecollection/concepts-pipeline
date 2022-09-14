package weco.concepts.ingestor

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import grizzled.slf4j.Logging
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import weco.concepts.common.elasticsearch.Indexer

import scala.concurrent.ExecutionContext

object Main extends App with Logging {
  val config = ConfigFactory.load()
  val lcshUrl = config.as[String]("data-source.loc.lcsh")
  val lcNamesUrl = config.as[String]("data-source.loc.names")
  val indexName = config.as[String]("data-target.index.name")
  val maxBulkRecords = config.as[Int]("data-target.bulk.max-records")
  val clusterConfig =
    config.as[Indexer.ClusterConfig]("data-target.cluster")

  implicit val actorSystem: ActorSystem = ActorSystem("main")
  implicit val executionContext: ExecutionContext = actorSystem.dispatcher

  val indexer = Indexer(clusterConfig)
  val ingestStream = new IngestStream(
    subjectsUrl = lcshUrl,
    namesUrl = lcNamesUrl,
    indexer = indexer,
    indexName = indexName,
    maxRecordsPerBulkRequest = maxBulkRecords
  )
  ingestStream.run
    .recover(err => error(err.getMessage))
    .onComplete(_ => actorSystem.terminate())
}
