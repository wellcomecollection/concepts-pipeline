package weco.concepts.recorder

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, ZipWith}
import grizzled.slf4j.Logging
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
) extends Logging {
  private lazy val mget = new MultiGetFlow(
    elasticHttpClient = elasticHttpClient,
    maxBatchSize = maxRecordsPerBulkRequest
  )
  private lazy val bulkUpdateFlow = new BulkUpdateFlow[Concept](
    elasticHttpClient = elasticHttpClient,
    indexName = targetIndexName,
    maxBulkRecords = maxRecordsPerBulkRequest
  ).flow

  /*
   * This flow is constructed of a graph that looks like this:
   *
   *
   *                   - (get authoritative) -
   *                 /                         \
   * (doc ids) --> ()                       (merge) --> (update)
   *                 \                         /
   *                   ----- (get used) ------
   */
  def recordIds: Flow[String, BulkUpdateResult, NotUsed] =
    Flow.fromGraph(GraphDSL.createGraph(bulkUpdateFlow) {
      implicit builder => bulkUpdate =>
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
        merge.out ~> bulkUpdate

        FlowShape(fork.in, bulkUpdate.out)
    })
}
