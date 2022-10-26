package weco.concepts.ingestor.stages

import akka.NotUsed
import akka.stream.scaladsl._
import grizzled.slf4j.Logging
import weco.concepts.common.json.JsonOps._
import weco.concepts.common.model._

trait Transformer[SourceType <: IdentifierType] {
  def transform(sourceString: String): Option[AuthoritativeConcept]
}

object Transformer extends Logging {
  def apply[T <: IdentifierType: Transformer]
    : Flow[String, AuthoritativeConcept, NotUsed] =
    Flow
      .fromFunction(implicitly[Transformer[T]].transform)
      .mapConcat {
        case Some(concept) => Seq(concept)
        case None          => Nil
      }

  implicit val subjectsTransformer
    : Transformer[IdentifierType.LCSubjects.type] =
    libraryOfCongressJsonLdSkosTransformer(IdentifierType.LCSubjects)
  implicit val namesTransformer: Transformer[IdentifierType.LCNames.type] =
    libraryOfCongressJsonLdSkosTransformer(IdentifierType.LCNames)

  def libraryOfCongressJsonLdSkosTransformer[T <: IdentifierType](
    identifierType: T
  ): Transformer[T] =
    (sourceString: String) => {
      val json = ujson.read(sourceString)
      for {
        // The lines in the bulk exports typically have 3 top-level fields:
        // "@id", "@graph", "@context"
        // The @id field takes the form of a path, eg:
        // /authorities/subjects/sh12345678
        // /authorities/names/na12345678
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
        // The prefLabel is non-optional; sometimes it's a string and sometimes it's an
        // object with a @value
        label <- node
          .opt[String]("skos:prefLabel")
          .orElse(node.opt[String]("skos:prefLabel", "@value"))
        // We might not have any altLabels - that's OK, we can just use an empty list
        altLabels = node.optSeq("skos:altLabel").getOrElse(Nil)
        // Again, altLabels might be plain strings or they could be objects with @values
        altLabelValues = altLabels.map(label =>
          label
            .opt[String]
            .orElse(label.opt[String]("@value"))
        )
      } yield AuthoritativeConcept(
        identifier = Identifier(
          // As above, this is initially a path: the leaf is all we care about,
          // because our IdentifierType tells us the relevant context
          value = conceptId.split('/').last,
          identifierType = identifierType
        ),
        label = label,
        alternativeLabels = altLabelValues.flatten
      )
    }

}
