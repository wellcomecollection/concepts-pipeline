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
  lazy val subjectsSource: Source[AuthoritativeConcept, NotUsed] =
    conceptSource[IdentifierType.LCSubjects.type](subjectsUrl)
  lazy val namesSource: Source[AuthoritativeConcept, NotUsed] =
    conceptSource[IdentifierType.LCNames.type](namesUrl)
  lazy val bulkUpdater = new BulkUpdateFlow[AuthoritativeConcept](
    elasticHttpClient = elasticHttpClient,
    maxBulkRecords = maxRecordsPerBulkRequest,
    indexName = indexName,
    filterDocuments = filterConcepts
  )
  lazy val indices = new Indices(elasticHttpClient)

  def run: Future[Done] = {
    indices.create(indexName).flatMap { _ =>
      Source
        .combine(subjectsSource, namesSource)(Merge(_))
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
  ): Source[AuthoritativeConcept, NotUsed] =
    fetcher
      .fetchFromUrl(dataUrl)
      // At time of writing,
      // The largest JSON in LC-Subjects is ca 77KiB
      // So 128KiB should give sufficient overhead to catch any expansion
      .via(Scroll(128 * 1024))
      .via(Transformer.apply[T])

  // Some LCSH identifiers have a suffix, `-781`
  // This seems to be a way of representing a subdivision
  // linking for geographic entities: in practice, an alternative
  // name for a place. We don't really care about these
  // as they're not used in our catalogue, and it would be non-trivial
  // to work out how to merge the subdivisions with their parents,
  // so we just filter them out here.
  private def filterConcepts(concept: AuthoritativeConcept): Boolean =
    concept.identifier match {
      case Identifier(value, IdentifierType.LCSubjects, _)
          if value.endsWith("-781") =>
        false
      case _ => true
    }
}
