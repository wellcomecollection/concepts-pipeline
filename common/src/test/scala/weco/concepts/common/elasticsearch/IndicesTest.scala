package weco.concepts.common.elasticsearch

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import org.scalatest.funspec.AsyncFunSpec
import org.scalatest.matchers.should.Matchers
import weco.concepts.common.ResourceLoader
import weco.concepts.common.fixtures.TestElasticHttpClient

import scala.util.Success

class IndicesTest extends AsyncFunSpec with Matchers {
  implicit val actorSystem: ActorSystem = ActorSystem("test")
  val indexName = "test-index"
  val indexConfig = ""

  implicit val loader: ResourceLoader = new TestResourceLoader(
    mapping = Map(
      indexName -> ""
    )
  )

  it("creates a new index") {
    val client = TestElasticHttpClient({
      case HttpRequest(HttpMethods.PUT, uri, _, _, _)
          if uri.path.toString() == s"/$indexName" =>
        Success(HttpResponse(StatusCodes.OK))
    })
    val indices = new Indices(client)

    indices.create(indexName) map { result =>
      result shouldBe Done
    }
  }

  it("raises a helpful exception if Elasticsearch says no.") {
    val client = TestElasticHttpClient({
      case HttpRequest(HttpMethods.PUT, uri, _, _, _)
          if uri.path.toString() == s"/$indexName" =>
        Success(
          HttpResponse(
            StatusCodes.BadRequest,
            entity = HttpEntity(
              ContentTypes.`application/json`,
              s"""{
            "error": {
              "reason": "I'm sorry, Dave. I'm afraid I can't do that",
            },
            "status": 400
          }""".stripMargin
            )
          )
        )
    })
    val indices = new Indices(client)
    indices.create(indexName).failed.map { result =>
      result shouldBe a[RuntimeException]
      val message: String = result.getMessage
      message should include(s"/$indexName")
      message should include("400")
      message should include("I'm sorry, Dave. I'm afraid I can't do that")
    }
  }

  it("does not error for an existing index") {
    val client = TestElasticHttpClient({
      case HttpRequest(HttpMethods.PUT, uri, _, _, _)
          if uri.path.toString() == s"/$indexName" =>
        Success(
          HttpResponse(
            StatusCodes.BadRequest,
            entity = HttpEntity(
              ContentTypes.`application/json`,
              s"""{
                "error": {
                  "root_cause": [
                    {
                      "type": "resource_already_exists_exception",
                      "reason": "index [$indexName/_0yO5D0ZQbyXBy2CdZ5hIA] already exists",
                      "index_uuid": "_0yO5D0ZQbyXBy2CdZ5hIA",
                      "index": "$indexName"
                    }
                  ],
                  "type": "resource_already_exists_exception",
                  "reason": "index [$indexName/_0yO5D0ZQbyXBy2CdZ5hIA] already exists",
                  "index_uuid": "_0yO5D0ZQbyXBy2CdZ5hIA",
                  "index": "$indexName"
                },
                "status": 400
              }""".stripMargin
            )
          )
        )
    })
    val indices = new Indices(client)

    indices.create(indexName) map { result =>
      result shouldBe Done
    }
  }
}
