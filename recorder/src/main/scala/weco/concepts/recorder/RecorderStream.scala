package weco.concepts.recorder

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, ZipWith}
import weco.concepts.common.elasticsearch.{
  BulkUpdateFlow,
  BulkUpdateResult,
  ElasticHttpClient
}
import weco.concepts.common.model.{AuthoritativeConcept, Concept, UsedConcept}

class RecorderStream(
  authoritativeConceptsIndexName: String,
  usedConceptsIndexName: String,
  targetIndexName: String,
  elasticHttpClient: ElasticHttpClient,
  maxRecordsPerBulkRequest: Int = 1000
) {
  private lazy val mget = new MultiGetFlow(
    elasticHttpClient = elasticHttpClient,
    maxBatchSize = maxRecordsPerBulkRequest
  )
  private lazy val bulkUpdateFlow = new BulkUpdateFlow[Concept](
    elasticHttpClient = elasticHttpClient,
    indexName = targetIndexName,
    maxBulkRecords = maxRecordsPerBulkRequest
  ).flow

  def recordIds: Flow[String, BulkUpdateResult, NotUsed] =
    Flow.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val fork = builder.add(Broadcast[String](2))
      val merge = builder.add(
        ZipWith(MergeConcepts(_, _))
      )

      val getAuthoritativeConcept =
        mget.forIndex[AuthoritativeConcept](authoritativeConceptsIndexName)
      val getUsedConcept = mget.forIndex[UsedConcept](usedConceptsIndexName)

      fork.out(1) ~> getAuthoritativeConcept ~> merge.in0
      fork.out(0) ~> getUsedConcept ~> merge.in1
      merge.out ~> bulkUpdateFlow

      FlowShape(fork.in, bulkUpdateFlow.shape.out)
    })
}
