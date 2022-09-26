package weco.concepts.common.elasticsearch

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import org.scalatest.funspec.AsyncFunSpec
import org.scalatest.matchers.should.Matchers
import weco.concepts.common.fixtures.TestElasticHttpClient

import scala.util.Success

class BulkUpdateFlowTest extends AsyncFunSpec with Matchers {
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
  object TestDocFormatter extends BulkFormatter[TestDoc] {
    def identifier(item: TestDoc): String = item.id
    def doc(item: TestDoc): ujson.Obj =
      ujson.Obj("id" -> item.id, "value" -> item.value)
  }

  it("bulk indexes records in groups") {
    val groupSize = 10
    val nDocs = 1000

    val bulkUpdateFlow = new BulkUpdateFlow[TestDoc](
      formatter = TestDocFormatter,
      max_bulk_records = groupSize,
      elasticHttpClient = client,
      indexName = "test-index"
    )
    val documents = (1 to nDocs).map(i => TestDoc(id = i.toString, value = i))

    val results = Source(documents)
      .via(bulkUpdateFlow.flow)
      .runWith(TestSink.probe[Map[String, Int]])
      .request(nDocs / groupSize)
      .expectNextN(nDocs / groupSize)

    results.map(_.getOrElse("total", 0)).sum shouldBe nDocs
    client.requests.length shouldBe nDocs / groupSize
  }
}
