package weco.concepts.aggregator

import com.sksamuel.elastic4s.ElasticApi
import com.sksamuel.elastic4s.requests.update.UpdateRequest
import weco.concepts.common.model.UsedConcept

/** Formatter to turn concepts into couplets for use in an Elasticsearch Bulk
  * request.
  */
class BulkFormatter(index: String) {
  def format(concept: UsedConcept): UpdateRequest = {
    ElasticApi
      .updateById(
        index,
        id = concept.identifier.toString
      )
      .docAsUpsert(
        "authority" -> concept.identifier.identifierType.toString,
        "identifier" -> concept.identifier.value,
        "label" -> concept.label,
        "canonicalId" -> concept.canonicalId
      )
  }
}
