package weco.concepts.ingestor

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.scaladsl._
import grizzled.slf4j.Logging
import weco.concepts.common.elasticsearch.{BulkUpdateFlow, Indexer}
import weco.concepts.common.model._
import weco.concepts.common.source.{Fetcher, Scroll}
import weco.concepts.ingestor.stages.Transformer

import scala.concurrent.{ExecutionContext, Future}

class IngestStream(
  subjectsUrl: String,
  namesUrl: String,
  indexer: Indexer,
  indexName: String,
  maxRecordsPerBulkRequest: Int
)(implicit
  actorSystem: ActorSystem
) extends Logging {
  implicit val executionContext: ExecutionContext = actorSystem.dispatcher

  lazy val fetcher = new Fetcher(Http().superPool())
  lazy val subjectsSource: Source[Concept, NotUsed] =
    conceptSource[IdentifierType.LCSubjects.type](subjectsUrl)
  lazy val namesSource: Source[Concept, NotUsed] =
    conceptSource[IdentifierType.LCNames.type](namesUrl)
  lazy val bulkUpdater = new BulkUpdateFlow(
    formatter = ConceptFormatter,
    max_bulk_records = maxRecordsPerBulkRequest,
    indexer = indexer,
    indexName = indexName
  )

  def run: Future[Done] =
    Source
      .combine(subjectsSource, namesSource)(Merge(_))
      .via(bulkUpdater.flow)
      .runWith(Sink.fold(0L)((n, _) => n + 1))
      .map(n => {
        info(s"Indexed $n concepts from $subjectsUrl and $namesUrl")
        Done
      })

  private def conceptSource[T <: IdentifierType: Transformer](
    dataUrl: String
  ): Source[Concept, NotUsed] =
    fetcher
      .fetchFromUrl(dataUrl)
      // At time of writing,
      // The largest JSON in LC-Subjects is ca 77KiB
      // So 128KiB should give sufficient overhead to catch any expansion
      .via(Scroll(128 * 1024))
      .via(Transformer.apply[T])
}
