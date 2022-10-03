package weco.concepts.ingestor

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.scaladsl._
import grizzled.slf4j.Logging
import weco.concepts.common.elasticsearch.{
  BulkUpdateFlow,
  ElasticHttpClient,
  Indices
}
import weco.concepts.common.model._
import weco.concepts.common.source.{Fetcher, Scroll}
import weco.concepts.ingestor.stages.Transformer

import scala.concurrent.{ExecutionContext, Future}

class IngestStream(
  subjectsUrl: String,
  namesUrl: String,
  elasticHttpClient: ElasticHttpClient,
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
    elasticHttpClient = elasticHttpClient,
    indexName = indexName
  )
  lazy val indices = new Indices(elasticHttpClient)

  def run: Future[Done] = {
    indices.create(indexName).flatMap { _ =>
      Source
        .combine(subjectsSource, namesSource)(Merge(_))
        // TODO: This is a temporary restriction I have added while refactoring
        //   and Lambda-izing, so that I can "run" to make sure I haven't completely
        //   broken it by disconnection things.
        //   It might be nice to make it selectable so that we can have a quick e2e
        //   test.
        // .take(10)
        .via(bulkUpdater.flow)
        .runWith(Sink.fold(0L)((n, _) => n + 1))
        .map(n => {
          info(s"Indexed $n concepts from $subjectsUrl and $namesUrl")
          Done
        })
    }
  }

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
