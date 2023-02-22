package weco.concepts.common.elasticsearch

import weco.concepts.common.json.Indexable
import weco.concepts.common.json.Indexable.IndexableOps

class ScriptedBulkUpdateFlow[T: Indexable](
  elasticHttpClient: ElasticHttpClient,
  maxBulkRecords: Int,
  indexName: String,
  scriptName: String,
  filterDocuments: T => Boolean = (_: T) => true
) extends BulkUpdateFlow[T](
      elasticHttpClient,
      maxBulkRecords,
      indexName,
      filterDocuments
    ) {

  override def format(item: T): String = {
    val action = ujson.Obj(
      "update" -> ujson.Obj(
        "_index" -> indexName,
        "_id" -> item.id
      )
    )
    val document = ujson.Obj(
      "script" -> ujson.Obj(
        "id" -> scriptName,
        "params" -> item.toUpdateParams
      ),
      "upsert" -> item.toDoc
    )
    s"${action.render()}\n${document.render()}"
  }

}
