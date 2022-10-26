package weco.concepts.aggregator

import weco.concepts.common.elasticsearch.{BulkUpdateFlow, ElasticHttpClient}
import weco.concepts.common.model.UsedConcept
import weco.concepts.common.json.Indexable._

class UsedConceptBulkUpdateFlow(
  elasticHttpClient: ElasticHttpClient,
  maxBulkRecords: Int,
  indexName: String
) extends BulkUpdateFlow[UsedConcept](
      elasticHttpClient,
      maxBulkRecords,
      indexName
    ) {

  def identifier(concept: UsedConcept): Option[String] = Some(concept.id)
  def doc(concept: UsedConcept): Option[ujson.Value] = Some(concept.toDoc)
}
