package weco.concepts.common.elasticsearch

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import org.scalatest.funspec.AsyncFunSpec
import org.scalatest.matchers.should.Matchers
import weco.concepts.common.ResourceLoader
import weco.concepts.common.fixtures.TestElasticHttpClient

import scala.util.Success

class TestResourceLoader(mapping: Map[String, String]) extends ResourceLoader {
  override def loadJsonResource(name: String): String = mapping(name)
}

class ScriptsTest extends AsyncFunSpec with Matchers {
  implicit val actorSystem: ActorSystem = ActorSystem("test")
  val scriptName = "my-script"
  val context = "aggregation_selector"

  implicit val loader: ResourceLoader = new TestResourceLoader(
    mapping = Map(
      scriptName ->
        """
    "script": {
      "lang": "painless",
      "source": "We should have thought of it a million years ago, in the nineties."
    }
  """
    )
  )

  it("creates a new script") {

    val client = TestElasticHttpClient({
      case HttpRequest(HttpMethods.PUT, uri, _, _, _)
          if uri.path.toString() == s"/_scripts/$scriptName/$context" =>
        Success(HttpResponse(StatusCodes.OK))
    })
    val scripts = new Scripts(client)
    scripts.create(scriptName, context) map { result =>
      result shouldBe Done
    }
  }

  it("raises a helpful exception if Elasticsearch says no.") {
    val client = TestElasticHttpClient({
      case HttpRequest(HttpMethods.PUT, uri, _, _, _)
          if uri.path.toString() == s"/_scripts/$scriptName/$context" =>
        Success(
          HttpResponse(
            StatusCodes.BadRequest,
            entity = HttpEntity(
              ContentTypes.`application/json`,
              """{
                    "error": {
                      "root_cause": ["I can't find my pulverizer!"]
                    }
                 }
              """
            )
          )
        )
    })

    new Scripts(client).create(scriptName, context).failed.map { result =>
      result shouldBe a[RuntimeException]
      val message: String = result.getMessage
      message should include(s"/_scripts/$scriptName/$context")
      message should include("400")
      message should include("I can't find my pulverizer!")
    }
  }

}
