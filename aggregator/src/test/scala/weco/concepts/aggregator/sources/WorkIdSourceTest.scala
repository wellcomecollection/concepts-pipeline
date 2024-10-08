package weco.concepts.aggregator.sources

import org.apache.pekko.NotUsed
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import org.apache.pekko.stream.scaladsl.Flow
import org.apache.pekko.stream.testkit.scaladsl.TestSink
import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers

import scala.util.{Success, Try}

class WorkIdSourceTest extends AnyFeatureSpec with Matchers with GivenWhenThen {
  implicit val actorSystem: ActorSystem = ActorSystem("test")

  def doNotFetchFlow()
    : Flow[(HttpRequest, String), (Try[HttpResponse], String), NotUsed] =
    Flow.fromFunction(_ => fail())

  def mockFetchFlow()
    : Flow[(HttpRequest, String), (Try[HttpResponse], String), NotUsed] =
    Flow.fromFunction(
      { case (rq: HttpRequest, _) =>
        (
          Success(
            HttpResponse(
              StatusCodes.OK,
              entity = s"""{"id":"${rq.uri.path
                  .toString()
                  .split('/')
                  .last}"}"""
            )
          ),
          ""
        )
      }
    )

  Feature("Fetching Work documents by id") {

    Scenario("Nothing to do") {
      Given("no works to fetch")
      val workIds = Iterator.empty[String]
      Then("no Work documents are returned")
      val workIdSource: WorkIdSource = new WorkIdSource(
        workUrlTemplate = "http://example.com/%s",
        httpFlow = doNotFetchFlow()
      )
      workIdSource(workIds)
        .runWith(TestSink[String]())
        .request(1)
        .expectComplete()
    }

    Scenario("Fetching a single work") {
      Given("one work to fetch")
      val workIds = Iterator.single("g00dcafe")
      Then("one Work document is returned")
      val workIdSource: WorkIdSource = new WorkIdSource(
        workUrlTemplate = "http://example.com/%s",
        httpFlow = mockFetchFlow()
      )
      workIdSource(workIds)
        .runWith(TestSink[String]())
        .request(1)
        .expectNext("""{"id":"g00dcafe"}""")
        .expectComplete()

    }

    Scenario("Fetching multiple works") {
      Given("three works to fetch")
      val workIds = Iterator("g00dcafe", "g00df00d", "cafef00d")
      Then("three Work documents are returned")
      val workIdSource: WorkIdSource = new WorkIdSource(
        workUrlTemplate = "http://example.com/%s",
        httpFlow = mockFetchFlow()
      )
      workIdSource(workIds)
        .runWith(TestSink[String]())
        .request(3)
        .expectNext(
          """{"id":"g00dcafe"}""",
          """{"id":"g00df00d"}""",
          """{"id":"cafef00d"}"""
        )
        .expectComplete()
    }

    Scenario("Filtering duplicate requests") {
      Given("five works to fetch")
      And("two of them are duplicates")
      val workIds =
        Iterator("g00dcafe", "g00df00d", "g00df00d", "cafef00d", "g00dcafe")
      Then("three Work documents are returned")
      val workIdSource: WorkIdSource = new WorkIdSource(
        workUrlTemplate = "http://example.com/%s",
        httpFlow = mockFetchFlow()
      )
      workIdSource(workIds)
        .runWith(TestSink[String]())
        .request(3)
        .expectNext(
          """{"id":"g00dcafe"}""",
          """{"id":"g00df00d"}""",
          """{"id":"cafef00d"}"""
        )
        .expectComplete()
    }
  }

}
