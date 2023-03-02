package weco.concepts.aggregator
import akka.actor.ActorSystem
import akka.http.scaladsl.model.{
  ContentTypes,
  HttpEntity,
  HttpMethods,
  HttpRequest,
  HttpResponse,
  StatusCodes
}
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.featurespec.AnyFeatureSpec
import weco.concepts.aggregator.testhelpers.ValueGenerators
import weco.concepts.common.elasticsearch.ElasticHttpClient
import weco.concepts.common.fixtures.TestElasticHttpClient
import weco.concepts.common.model.{CatalogueConcept, Identifier, IdentifierType}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success

class NotInIndexFlowTest
    extends AnyFeatureSpec
    with Matchers
    with GivenWhenThen
    with ValueGenerators {

  private implicit val actorSystem: ActorSystem = ActorSystem("test")
  private val indexName: String = "my-test-index"

  private def givenSomeConcepts(n: Int): Seq[CatalogueConcept] =
    (101 to (100 + n)).map(i =>
      CatalogueConcept(
        identifier = Identifier(
          value = s"n12345678$i",
          identifierType = IdentifierType.LCNames
        ),
        label = s"The concept of $i",
        canonicalId = s"abcdef$i",
        ontologyType = "Concept"
      )
    )
  private def givenAnIndex(queryResponse: String): TestElasticHttpClient =
    TestElasticHttpClient({
      case HttpRequest(HttpMethods.POST, uri, _, _, _)
          if uri.path
            .toString() == s"/$indexName/_search" &&
            uri.rawQueryString.get == "filter_path=hits.hits.fields.canonicalId" =>
        Success(
          HttpResponse(
            StatusCodes.OK,
            entity = HttpEntity(
              ContentTypes.`application/json`,
              queryResponse
            )
          )
        )
    })

  private def givenAnEmptyIndex: TestElasticHttpClient =
    givenAnIndex("{}")

  private def givenAnIndex(responseHits: Seq[String]): TestElasticHttpClient =
    givenAnIndex(
      s"""
                    {
                      "hits": {
                        "hits": [
                        ${responseHits.mkString(",\n")}
                        ]  
                        }
                    }
                    """
    )

  def runFlow(
    sourceRecords: Seq[CatalogueConcept],
    client: ElasticHttpClient,
    expectedCount: Int
  ): Seq[CatalogueConcept] =
    Source(sourceRecords)
      .via(
        new NotInIndexFlow(
          elasticHttpClient = client,
          indexName = indexName
        ).flow
      )
      .runWith(TestSink[CatalogueConcept]())
      .request(sourceRecords.length)
      .expectNextN(expectedCount)

  Feature("Filtering Concepts already in the index") {
    info(
      """
     | NotInIndexFlow consumes Concepts and only emits those that have not already been indexed.
     | It does this by checking for the Concept's canonicalId
      """.stripMargin
    )

    Scenario(s"An empty index") {
      Given("some concepts")
      val catalogueConcepts = givenSomeConcepts(10)

      And("an index with no data")
      val client = givenAnEmptyIndex

      When("NotInIndexFlow is run")
      val results = runFlow(catalogueConcepts, client, 10)
      Then("all the concepts are returned")
      results should equal(catalogueConcepts)
    }

    Scenario(s"All Concepts are already indexed") {
      Given("some concepts")
      val catalogueConcepts = givenSomeConcepts(10)

      And("an index with all of them in it")
      val hits = catalogueConcepts.map(concept =>
        s"""{"fields": {"canonicalId": ["${concept.canonicalId}"]}}"""
      )

      val client = givenAnIndex(hits)

      When("NotInIndexFlow is run")
      val results = runFlow(catalogueConcepts, client, 0)
      Then("no concepts are returned")
      results shouldBe Nil
    }

    Scenario(s"Some Concepts are already indexed") {
      Given("some concepts")
      val catalogueConcepts = givenSomeConcepts(10)

      And("an index with some of them in it")
      val (storedConcepts, newConcepts) = catalogueConcepts.splitAt(5)
      val hits = storedConcepts.map(concept =>
        s"""{"fields": {"canonicalId": ["${concept.canonicalId}"]}}"""
      )

      val client = givenAnIndex(hits)

      When("NotInIndexFlow is run")
      val results = runFlow(catalogueConcepts, client, 5)
      Then("only the concepts not in the index are returned")
      results shouldBe newConcepts
    }

    Scenario(s"Some Works have multiple Concepts") {
      Given("some concepts")
      val catalogueConcepts = givenSomeConcepts(10)

      And("an index with some of them in one Work")
      val (storedConcepts, newConcepts) = catalogueConcepts.splitAt(5)

      val client = givenAnIndex(Seq(s"""
        {
          "fields": {
            "canonicalId":${storedConcepts
          .map(_.canonicalId)
          .mkString("[\"", "\",\"", "\"]")}
          }
        }
      """))

      When("NotInIndexFlow is run")
      val results = runFlow(catalogueConcepts, client, 5)
      Then("only the concepts not in the index are returned")
      results shouldBe newConcepts
    }

  }
}
