package weco.concepts.ingestor

import weco.concepts.common.elasticsearch.BulkFormatter
import weco.concepts.common.model.{Concept, Identifier, IdentifierType}

object ConceptFormatter extends BulkFormatter[Concept] {
  // Some LCSH identifiers have a suffix, `-781`
  // This seems to be a way of representing a subdivision
  // linking for geographic entities: in practice, an alternative
  // name for a place. We don't really care about these
  // as they're not used in our catalogue, and it would be non-trivial
  // to work out how to merge the subdivisions with their parents,
  // so we just filter them out here.
  def identifier(concept: Concept): Option[String] = concept.identifier match {
    case Identifier(value, IdentifierType.LCSubjects, _)
        if value.endsWith("-781") =>
      None
    case id => Some(id.toString)
  }

  def doc(concept: Concept): Option[ujson.Obj] = Some(
    ujson.Obj(
      "authority" -> concept.identifier.identifierType.id,
      "identifier" -> concept.identifier.value,
      "label" -> concept.label,
      "alternativeLabels" -> concept.alternativeLabels
    )
  )
}
