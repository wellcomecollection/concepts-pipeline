package weco.concepts.aggregator

import weco.concepts.common.model.UsedConcept

/** Formatter to turn concepts into couplets for use in an Elasticsearch Bulk
  * update request.
  */
class BulkFormatter(index: String) {
  def format(concept: UsedConcept): String = {
    val action = ujson.Obj(
      "update" -> ujson.Obj(
        "_index" -> index,
        "_id" -> concept.identifier.toString
      )
    )
    val document = ujson.Obj(
      "doc_as_upsert" -> true,
      "doc" -> ujson.Obj(
        "authority" -> concept.identifier.identifierType.id,
        "identifier" -> concept.identifier.value,
        "label" -> concept.label,
        "canonicalId" -> concept.canonicalId
      )
    )
    s"${action.render()}\n${document.render()}"
  }
}
