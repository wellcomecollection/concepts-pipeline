package weco.concepts.recorder

import akka.NotUsed
import akka.stream.{FlowShape, Materializer, OverflowStrategy, SourceShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Source, ZipWith}
import grizzled.slf4j.Logging
import weco.concepts.common.elasticsearch.{
  BulkUpdateFlow,
  BulkUpdateResult,
  ElasticHttpClient
}
import weco.concepts.common.model.{AuthoritativeConcept, Concept, UsedConcept}
import weco.concepts.common.json.Indexable._

import scala.concurrent.ExecutionContext

class RecorderStream(
  authoritativeConceptsIndexName: String,
  usedConceptsIndexName: String,
  targetIndexName: String,
  elasticHttpClient: ElasticHttpClient,
  maxRecordsPerBulkRequest: Int = 1000
)(implicit mat: Materializer)
    extends Logging {
  private lazy val mget = new MultiGetFlow(
    elasticHttpClient = elasticHttpClient,
    maxBatchSize = maxRecordsPerBulkRequest
  )
  private lazy val bulkUpdateFlow = new BulkUpdateFlow[Concept](
    elasticHttpClient = elasticHttpClient,
    indexName = targetIndexName,
    maxBulkRecords = maxRecordsPerBulkRequest
  ).flow

  private implicit val ec: ExecutionContext = mat.executionContext
  def recordAllUsedConcepts: Source[BulkUpdateResult, NotUsed] =
    Source.fromGraph(GraphDSL.createGraph(bulkUpdateFlow) {
      implicit builder => bulkUpdate =>
        import GraphDSL.Implicits._

        val source = IndexSource[UsedConcept](
          elasticHttpClient,
          usedConceptsIndexName,
          pageSize = maxRecordsPerBulkRequest
        )

        val fork = builder.add(Broadcast[UsedConcept](2))
        val getUsedConceptId = Flow[UsedConcept].map(_.id)
        val toOptionBuffer = Flow[UsedConcept]
          .map(Option(_))
          // This buffer is necessary because, without it, the combination of
          // the `Broadcast`, the `grouped()` in the mget flow, and the `ZipWith`
          // stage afterwards causes a deadlock: Broadcast will not request
          // an element from the source until *both* downstream sides are ready for
          // it, ZipWith will not emit until both its upstreams have completed,
          // and grouped will not emit unless there is demand from the downstream.
          // This means that a buffer is required for the non-mget half of the flow
          // to keep receiving elements while the mget is waiting for the rest of the group.
          .buffer(maxRecordsPerBulkRequest, OverflowStrategy.backpressure)
        val getAuthoritativeConcept =
          mget.forIndex[AuthoritativeConcept](authoritativeConceptsIndexName)

        val merge = builder.add(ZipWith(MergeConcepts(_, _)))

        source ~> fork.in
        fork.out(0) ~> getUsedConceptId ~> getAuthoritativeConcept ~> merge.in0
        fork.out(1) ~> toOptionBuffer ~> merge.in1
        merge.out ~> bulkUpdate

        SourceShape(bulkUpdate.out)
    })

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
