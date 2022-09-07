package weco.concepts.aggregator

import weco.concepts.common.elasticsearch.BulkFormatter
import weco.concepts.common.model.UsedConcept

/** Formatter to turn concepts into couplets for use in an Elasticsearch Bulk
  * update request.
  */
object UsedConceptFormatter extends BulkFormatter[UsedConcept] {
  def identifier(concept: UsedConcept): String = concept.identifier.toString
  def doc(concept: UsedConcept): ujson.Obj = ujson.Obj(
    "authority" -> concept.identifier.identifierType.id,
    "identifier" -> concept.identifier.value,
    "label" -> concept.label,
    "canonicalId" -> concept.canonicalId
  )
}
