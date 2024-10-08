package weco.concepts.common.elasticsearch

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.unmarshalling.Unmarshal
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

class ScriptedBulkUpdateFlowTest extends AnyFunSpec with Matchers {
  implicit val actorSystem: ActorSystem = ActorSystem("test")
  val client = new TestElasticHttpClient(
    ElasticsearchResponses.handleBulkUpdate
  )

  case class TestDoc(id: String, value: String)

  object TestDoc {
    implicit val indexableTestDoc: Indexable[TestDoc] = new Indexable[TestDoc] {
      def id(t: TestDoc): String = t.id

      def toDoc(t: TestDoc): ujson.Value =
        ujson.Obj("id" -> t.id, "value" -> t.value)

      override def toUpdateParams(t: TestDoc): Value = ujson.Obj(
        "value" -> t.value
      )

      def fromDoc(doc: Value): Option[TestDoc] = throw new RuntimeException(
        "Not required!"
      )
    }
  }

  it("generates a script-based bulk update") {

    val bulkUpdateFlow = new ScriptedBulkUpdateFlow[TestDoc](
      elasticHttpClient = client,
      maxBulkRecords = 999,
      indexName = "test-index",
      scriptName = "my-script"
    )

    val results = Source(Seq(TestDoc(id = "hello", value = "world")))
      .via(bulkUpdateFlow.flow)
      .runWith(TestSink[BulkUpdateResult]())
      .request(1)
      .expectNextN(1)

    results.map(_.total).sum shouldBe 1
    client.requests.length shouldBe 1

    Unmarshal(client.requests.head.entity)
      .to[String]
      .map { responseBody: String =>
        val lines = responseBody.split('\n')
        ujson.read(lines.head) shouldEqual ujson.Obj(
          "update" -> ujson.Obj(
            "_index" -> "test-index",
            "_id" -> "hello"
          )
        )
        ujson.read(lines(1)) shouldEqual ujson.Obj(
          "script" -> ujson.Obj(
            "id" -> "my-script",
            "params" -> ujson.Obj("value" -> "world")
          ),
          "upsert" -> ujson.Obj("id" -> "hello", "value" -> "world")
        )
      }

  }
}
