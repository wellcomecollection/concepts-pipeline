package weco.concepts.common.elasticsearch

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.stream.testkit.scaladsl.TestSink
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import ujson.Value
import weco.concepts.common.fixtures.{
  ElasticsearchResponses,
  TestElasticHttpClient
}
import weco.concepts.common.json.Indexable

import scala.concurrent.ExecutionContext.Implicits.global

class BulkUpdateFlowTest extends AnyFunSpec with Matchers {
  implicit val actorSystem: ActorSystem = ActorSystem("test")
  val client = new TestElasticHttpClient(
    ElasticsearchResponses.handleBulkUpdate
  )

  case class TestDoc(id: String, value: Int)
  object TestDoc {
    implicit val indexableTestDoc: Indexable[TestDoc] = new Indexable[TestDoc] {
      def id(t: TestDoc): String = t.id
      def toDoc(t: TestDoc): ujson.Value =
        ujson.Obj("id" -> t.id, "value" -> t.value)

      def fromDoc(doc: Value): Option[TestDoc] = throw new RuntimeException(
        "Not required!"
      )
    }
  }

  it("bulk indexes records in groups") {
    val groupSize = 10
    val nDocs = 1000

    val bulkUpdateFlow = new BulkUpdateFlow[TestDoc](
      elasticHttpClient = client,
      maxBulkRecords = groupSize,
      indexName = "test-index"
    )
    val documents = (1 to nDocs).map(i => TestDoc(id = i.toString, value = i))

    val results = Source(documents)
      .via(bulkUpdateFlow.flow)
      .runWith(TestSink[BulkUpdateResult]())
      .request(nDocs / groupSize)
      .expectNextN(nDocs / groupSize)

    results.map(_.total).sum shouldBe nDocs
    client.requests.length shouldBe nDocs / groupSize
  }

  it("filters out items that aren't transformed successfully") {
    val bulkUpdateFlow = new BulkUpdateFlow[TestDoc](
      elasticHttpClient = client,
      maxBulkRecords = 10,
      indexName = "test-index",
      filterDocuments = _.id != "none"
    )
    val documents = Seq(
      TestDoc(id = "none", value = 123),
      TestDoc(id = "some", value = 456)
    )

    Source(documents)
      .via(bulkUpdateFlow.flow)
      .runWith(TestSink[BulkUpdateResult]())
      .request(1)
      .expectNext(
        BulkUpdateResult(
          took = 1234L,
          errored = Map.empty,
          updated = Seq("some"),
          noop = Nil
        )
      )
      .expectComplete()
  }

  describe("BulkUpdateResult") {
    it("parses updates and noops correctly") {
      val json =
        """
          |{
          |  "took": 1234,
          |  "errors": false,
          |  "items": [
          |    {
          |      "update": {
          |        "_index": "authoritative-concepts",
          |        "_id": "lc-names:n83217500",
          |        "_version": 1,
          |        "result": "noop",
          |        "_shards": {
          |          "total": 2,
          |          "successful": 1,
          |          "failed": 0
          |        },
          |        "_seq_no": 12345678,
          |        "_primary_term": 2,
          |        "status": 200
          |      }
          |    },
          |    {
          |      "update": {
          |        "_index": "authoritative-concepts",
          |        "_id": "lc-names:no2008068818",
          |        "_version": 2,
          |        "result": "updated",
          |        "_shards": {
          |          "total": 2,
          |          "successful": 1,
          |          "failed": 0
          |        },
          |        "_seq_no": 23456789,
          |        "_primary_term": 2,
          |        "status": 200
          |      }
          |    },
          |    {
          |      "update": {
          |        "_index": "authoritative-concepts",
          |        "_id": "lc-subjects:sh2003010454",
          |        "_version": 1,
          |        "result": "created",
          |        "_shards": {
          |          "total": 2,
          |          "successful": 1,
          |          "failed": 0
          |        },
          |        "_seq_no": 87654321,
          |        "_primary_term": 2,
          |        "status": 200
          |      }
          |    }
          |  ]
          |}""".stripMargin
      val result = BulkUpdateResult(ujson.read(json))

      result.took shouldBe 1234
      result.errored shouldBe empty
      result.updated should contain theSameElementsAs Seq(
        "lc-names:no2008068818",
        "lc-subjects:sh2003010454"
      )
      result.noop should contain only "lc-names:n83217500"
    }
    it("parses errors correctly") {
      val json = """
        |{
        |  "took": 1234,
        |  "errors": true,
        |  "items": [
        |    {
        |      "update": {
        |        "_index": "authoritative-concepts",
        |        "_id": "lc-names:n83217500",
        |        "_version": 1,
        |        "error": {
        |          "type": "strict_dynamic_mapping_exception",
        |          "reason": "mapping set to strict, dynamic introduction of [evil] within [_doc] is not allowed"
        |        },
        |        "status": 400
        |      }
        |    },
        |    {
        |      "update": {
        |        "_index": "authoritative-concepts",
        |        "_id": "lc-names:no2008068818",
        |        "_version": 2,
        |        "result": "updated",
        |        "_shards": {
        |          "total": 2,
        |          "successful": 1,
        |          "failed": 0
        |        },
        |        "_seq_no": 23456789,
        |        "_primary_term": 2,
        |        "status": 200
        |      }
        |    }
        |  ]
        |}""".stripMargin
      val result = BulkUpdateResult(ujson.read(json))

      result.errored.get("lc-names:n83217500") should contain(
        ujson.read(
          """
             |{
             |   "type": "strict_dynamic_mapping_exception",
             |   "reason": "mapping set to strict, dynamic introduction of [evil] within [_doc] is not allowed"
             |}""".stripMargin
        )
      )

      result.took shouldBe 1234
      result.updated should contain only "lc-names:no2008068818"
      result.noop shouldBe empty
    }
  }
}
