package weco.concepts.aggregator

import weco.concepts.common.elasticsearch.{BulkUpdateFlow, ElasticHttpClient}
import weco.concepts.common.model.UsedConcept

class UsedConceptBulkUpdateFlow(
  elasticHttpClient: ElasticHttpClient,
  maxBulkRecords: Int,
  indexName: String
) extends BulkUpdateFlow[UsedConcept](
      elasticHttpClient,
      maxBulkRecords,
      indexName
    ) {

  def identifier(concept: UsedConcept): Option[String] = Some(
    concept.identifier.toString
  )
  def doc(concept: UsedConcept): Option[ujson.Obj] = Some(
    ujson.Obj(
      "authority" -> concept.identifier.identifierType.id,
      "identifier" -> concept.identifier.value,
      "label" -> concept.label,
      "canonicalId" -> concept.canonicalId
    )
  )
}
