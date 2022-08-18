package weco.concepts.aggregator

import weco.concepts.common.model.UsedConcept

/** Formatter to turn concepts into couplets for use in an Elasticsearch Bulk
  * request.
  */
class BulkFormatter(index: String) {
  def format(concept: UsedConcept): String = {
    val action = ujson.Obj(
      "create" -> ujson.Obj(
        "_index" -> index,
        "_id" -> concept.identifier.toString
      )
    )
    val document = ujson.Obj(
      "identifier" -> concept.identifier.toString,
      "label" -> concept.label,
      "canonicalId" -> concept.canonicalId
    )
    s"${action.render()}\n${document.render()}"
  }
}
