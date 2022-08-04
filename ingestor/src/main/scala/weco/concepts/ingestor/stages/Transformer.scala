package weco.concepts.ingestor.stages

import akka.NotUsed
import akka.stream.scaladsl._
import grizzled.slf4j.Logging
import weco.concepts.ingestor.json.JsonOps._
import weco.concepts.ingestor.model._

trait Transformer[SourceType <: IdentifierType] {
  def transform(sourceString: String): Option[Concept]
}

object Transformer extends Logging {
  def apply[T <: IdentifierType: Transformer]: Flow[String, Concept, NotUsed] =
    Flow
      .fromFunction(implicitly[Transformer[T]].transform)
      .mapConcat {
        case Some(concept) => Seq(concept)
        case None          => Nil
      }

  implicit val lcshTransformer: Transformer[IdentifierType.LCSubjects.type] =
    (sourceString: String) => {
      val json = ujson.read(sourceString)
      for {
        conceptId <- json.opt[String]("@id")
        graph <- json.optSeq("@graph")
        node <- graph.find(node =>
          node
            .opt[String]("@id")
            .exists(graphNodeId => graphNodeId.endsWith(conceptId))
        )
        label <- node.opt[String]("skos:prefLabel", "@value")
        altLabels = node.optSeq("skos:altLabel").getOrElse(Nil)
        altLabelValues = altLabels.map(_.opt[String]("@value"))
      } yield Concept(
        identifier = Identifier(
          value = conceptId.split('/').last,
          identifierType = IdentifierType.LCSubjects
        ),
        label = label,
        alternativeLabels = altLabelValues.flatten
      )
    }

}
