package weco.concepts.ingestor

import weco.concepts.common.elasticsearch.BulkFormatter
import weco.concepts.common.model.Concept

object ConceptFormatter extends BulkFormatter[Concept] {
  def identifier(concept: Concept): String = concept.identifier.toString
  def doc(concept: Concept): ujson.Obj = ujson.Obj(
    "authority" -> concept.identifier.identifierType.id,
    "identifier" -> concept.identifier.value,
    "label" -> concept.label,
    "alternativeLabels" -> concept.alternativeLabels
  )
}
