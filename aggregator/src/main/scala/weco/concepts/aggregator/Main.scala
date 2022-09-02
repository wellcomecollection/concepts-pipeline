package weco.concepts.aggregator

import akka.NotUsed
import com.typesafe.config.ConfigFactory
import grizzled.slf4j.Logging
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

import scala.concurrent.ExecutionContext
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import weco.concepts.aggregator.sources._

object Main extends App with Logging {
  val config = ConfigFactory.load()
  lazy val snapshotUrl = config.as[String]("data-source.works.snapshot")
  lazy val workUrlTemplate = config.as[String]("data-source.workURL.template")
  lazy val maxFrameKiB = config.as[Int]("data-source.maxframe.kib")
  lazy val indexName = config.as[String]("data-target.index.name")
  lazy val maxBulkRecords = config.as[Int]("data-target.bulk.max-records")
  lazy val clusterConfig =
    config.as[Indexer.ClusterConfig]("data-target.cluster")

  implicit val actorSystem: ActorSystem = ActorSystem("main")
  implicit val executionContext: ExecutionContext = actorSystem.dispatcher

  // If you give it ids, it will fetch those records individually
  // If you don't it will either look at stdin or fetch the snapshot.
  val source: Source[String, NotUsed] =
    if (args.length > 0) WorkIdSource(args.iterator)
    else if (System.in.available() > 0) StdInSource.apply
    else WorksSnapshotSource(snapshotUrl)

  val indexer = Indexer(clusterConfig)
  val aggregator = new ConceptsAggregator(
    jsonSource = source,
    indexer = indexer,
    indexName = indexName,
    maxRecordsPerBulkRequest = maxBulkRecords
  )
  aggregator.run
    .recover(err => error(err.getMessage))
    .onComplete(_ => {
      indexer.close()
      actorSystem.terminate()
    })
}
