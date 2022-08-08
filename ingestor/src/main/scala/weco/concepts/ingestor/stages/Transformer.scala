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
        // The lines in the LCSH bulk exports typically have 3 top-level fields:
        // "@id", "@graph", "@context"
        // The @id field takes the form of a path, eg:
        // /authorities/subjects/sh12345678
        conceptId <- json.opt[String]("@id")
        // The @graph field is always a list of nodes, containing a mix of metadata,
        // links to other entries, and a root node with the ID specified at the top level
        graph <- json.optSeq("@graph")
        // The node ID is not just a path; it's the full URI. Eg:
        // http://id.loc.gov/authorities/subjects/sh12345678
        // We find the root node by checking for the path we got from the top of the document
        node <- graph.find(node =>
          node
            .opt[String]("@id")
            .exists(graphNodeId => graphNodeId.endsWith(conceptId))
        )
        // The prefLabel is non-optional
        label <- node.opt[String]("skos:prefLabel", "@value")
        // We might not have any altLabels - that's OK, we can just use an empty list
        altLabels = node.optSeq("skos:altLabel").getOrElse(Nil)
        altLabelValues = altLabels.map(_.opt[String]("@value"))
      } yield Concept(
        identifier = Identifier(
          // As above, this is initially a path: the leaf is all we care about,
          // because our IdentifierType tells us the relevant context
          value = conceptId.split('/').last,
          identifierType = IdentifierType.LCSubjects
        ),
        label = label,
        alternativeLabels = altLabelValues.flatten
      )
    }

}
