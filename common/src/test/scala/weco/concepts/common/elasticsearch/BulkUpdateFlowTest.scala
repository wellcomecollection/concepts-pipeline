package weco.concepts.common.elasticsearch

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.concepts.common.fixtures.TestElasticHttpClient

import scala.util.Success
import scala.concurrent.ExecutionContext.Implicits.global

class BulkUpdateFlowTest extends AnyFunSpec with Matchers {
  implicit val actorSystem: ActorSystem = ActorSystem("test")
  val client = new TestElasticHttpClient({
    case HttpRequest(HttpMethods.POST, uri, _, entity, _)
        if uri.path.toString() == "/_bulk" =>
      Unmarshal(entity).to[String].map { bulkUpdateRequest =>
        val couplets = bulkUpdateRequest
          .split('\n')
          .map(ujson.read(_))
          .grouped(2)
          .collect { case Array(a, b) => a -> b }
          .toSeq
        val result = ujson.Obj(
          "took" -> 1234,
          "errors" -> false,
          "items" -> couplets.map { case (action, _) =>
            ujson.Obj(
              "update" -> ujson.Obj(
                "_id" -> action("update")("_id").str,
                "result" -> "created"
              )
            )
          }
        )
        Success(
          HttpResponse(
            StatusCodes.OK,
            entity = HttpEntity(
              ContentTypes.`application/json`,
              ujson.write(result)
            )
          )
        )
      }
  })

  case class TestDoc(id: String, value: Int)

  class TestBulkUpdateFlow(
    elasticHttpClient: ElasticHttpClient,
    maxBulkRecords: Int,
    indexName: String
  ) extends BulkUpdateFlow[TestDoc](
        elasticHttpClient,
        maxBulkRecords,
        indexName
      ) {
    def identifier(item: TestDoc): Option[String] = item.id match {
      case "none" => None
      case id     => Some(id)
    }

    def doc(item: TestDoc): Option[ujson.Obj] =
      Some(ujson.Obj("id" -> item.id, "value" -> item.value))
  }

  it("bulk indexes records in groups") {
    val groupSize = 10
    val nDocs = 1000

    val bulkUpdateFlow = new TestBulkUpdateFlow(
      elasticHttpClient = client,
      maxBulkRecords = groupSize,
      indexName = "test-index"
    )
    val documents = (1 to nDocs).map(i => TestDoc(id = i.toString, value = i))

    val results = Source(documents)
      .via(bulkUpdateFlow.flow)
      .runWith(TestSink.probe[BulkUpdateResult])
      .request(nDocs / groupSize)
      .expectNextN(nDocs / groupSize)

    results.map(_.total).sum shouldBe nDocs
    client.requests.length shouldBe nDocs / groupSize
  }

  it("filters out items that aren't transformed successfully") {
    val bulkUpdateFlow = new TestBulkUpdateFlow(
      elasticHttpClient = client,
      maxBulkRecords = 10,
      indexName = "test-index"
    )
    val documents = Seq(
      TestDoc(id = "none", value = 123),
      TestDoc(id = "some", value = 456)
    )

    Source(documents)
      .via(bulkUpdateFlow.flow)
      .runWith(TestSink.probe[BulkUpdateResult])
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
