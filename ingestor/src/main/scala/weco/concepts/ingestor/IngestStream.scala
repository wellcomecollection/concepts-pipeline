package weco.concepts.ingestor

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.scaladsl._
import grizzled.slf4j.Logging
import weco.concepts.common.model._
import weco.concepts.ingestor.stages.{Fetcher, Scroll, Transformer}

import scala.concurrent.{ExecutionContext, Future}

class IngestStream(subjectsUrl: String, namesUrl: String)(implicit
  actorSystem: ActorSystem
) extends Logging {
  implicit val executionContext: ExecutionContext = actorSystem.dispatcher

  lazy val fetcher = new Fetcher(Http().superPool())
  lazy val subjectsSource: Source[Concept, NotUsed] =
    conceptSource[IdentifierType.LCSubjects.type](subjectsUrl)
  lazy val namesSource: Source[Concept, NotUsed] =
    conceptSource[IdentifierType.LCNames.type](namesUrl)

  def run: Future[Done] =
    Source
      .combine(subjectsSource, namesSource)(Merge(_))
      .runWith(Sink.fold(0L)((n, _) => n + 1))
      .map(n => {
        info(s"Extracted $n concepts from $subjectsUrl and $namesUrl")
        Done
      })

  private def conceptSource[T <: IdentifierType: Transformer](
    dataUrl: String
  ): Source[Concept, NotUsed] =
    fetcher
      .fetchFromUrl(dataUrl)
      .via(Scroll.apply)
      .via(Transformer.apply[T])
}
