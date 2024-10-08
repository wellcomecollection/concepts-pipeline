package weco.concepts.recorder

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.{
  FlowShape,
  Materializer,
  OverflowStrategy,
  SourceShape
}
import org.apache.pekko.stream.scaladsl.{
  Broadcast,
  Flow,
  GraphDSL,
  Source,
  ZipWith
}
import grizzled.slf4j.Logging
import weco.concepts.common.elasticsearch.{
  BulkUpdateFlow,
  BulkUpdateResult,
  ElasticHttpClient
}
import weco.concepts.common.model.{
  AuthoritativeConcept,
  CatalogueConcept,
  Concept
}
import weco.concepts.common.json.Indexable._

import scala.concurrent.ExecutionContext

class RecorderStream(
  authoritativeConceptsIndexName: String,
  catalogueConceptsIndexName: String,
  targetIndexName: String,
  elasticHttpClient: ElasticHttpClient,
  maxRecordsPerBulkRequest: Int = 1000
)(implicit mat: Materializer)
    extends Logging {

  private lazy val mget = new MultiGetFlow(
    elasticHttpClient = elasticHttpClient,
    maxBatchSize = maxRecordsPerBulkRequest
  )

  private lazy val bulkUpdateFlow = Flow[Seq[Concept]]
    .mapConcat(identity)
    .via(
      new BulkUpdateFlow[Concept](
        elasticHttpClient = elasticHttpClient,
        indexName = targetIndexName,
        maxBulkRecords = maxRecordsPerBulkRequest
      ).flow
    )

  private implicit val ec: ExecutionContext = mat.executionContext
  def recordAllCatalogueConcepts: Source[BulkUpdateResult, NotUsed] =
    Source.fromGraph(GraphDSL.createGraph(bulkUpdateFlow) {
      implicit builder => bulkUpdate =>
        import GraphDSL.Implicits._

        val source = IndexSource[CatalogueConcept](
          elasticHttpClient,
          catalogueConceptsIndexName,
          pageSize = maxRecordsPerBulkRequest
        )

        val fork = builder.add(Broadcast[CatalogueConcept](2))
        val getCatalogueConceptId = Flow[CatalogueConcept].map(_.id)
        val toOptionBuffer = Flow[CatalogueConcept]
          .map(Option(_))
          // This buffer is necessary because, without it, the combination of
          // the `Broadcast`, the `grouped()` in the mget flow, and the `ZipWith`
          // stage afterwards causes a deadlock:
          // 1. The `fork` stage `Broadcast`s one `CatalogueConcept` to `getCatalogueConceptId` and `toOptionBuffer`
          // 2. `getAuthoritativeConcept` waits to receive a full group (`maxRecordsPerBulkRequest`) before emitting
          // 3. The `ZipWith` in `merge` receives 1 element from `toOptionBuffer` but won't emit
          //    until it receives one from `getAuthoritativeConcept` as well.
          // 4. We are deadlocked: `getAuthoritativeConcept` is waiting for the rest of the group and so won't emit,
          //    meaning that `ZipWith` won't emit, `toOptionBuffer` is "full" and so the `Broadcast` won't emit. We
          //    need to introduce a buffer at least as big as the group size (minus one, for the "in-flight" element).
          .buffer(maxRecordsPerBulkRequest, OverflowStrategy.backpressure)
        val getAuthoritativeConcept =
          mget.forIndex[AuthoritativeConcept](authoritativeConceptsIndexName)

        val merge = builder.add(ZipWith(MergeConcepts(_, _)))

        source ~> fork.in
        fork.out(
          0
        ) ~> getCatalogueConceptId ~> getAuthoritativeConcept ~> merge.in0
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
        val getCatalogueConcept =
          mget.forIndex[CatalogueConcept](catalogueConceptsIndexName)

        fork.out(1) ~> getAuthoritativeConcept ~> merge.in0
        fork.out(0) ~> getCatalogueConcept ~> merge.in1
        merge.out ~> bulkUpdate

        FlowShape(fork.in, bulkUpdate.out)
    })
}
