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
import org.scalatest.LoneElement.convertToCollectionLoneElementWrapper

class NotInIndexFlowTest
    extends AnyFeatureSpec
    with Matchers
    with GivenWhenThen
    with ValueGenerators {

  private implicit val actorSystem: ActorSystem = ActorSystem("test")
  private val indexName: String = "my-test-index"

  private def givenSomeConcepts(n: Int): Seq[CatalogueConcept] =
    (101 to (100 + n)).map(i => givenAConcept(Seq(i.toString)))

  private def givenAMultiIdConcept(fromInt: Int, toInt: Int): CatalogueConcept =
    givenAConcept((fromInt to toInt).map(_.toString))

  private def givenAConcept(idSuffixes: Seq[String]): CatalogueConcept =
    CatalogueConcept(
      identifier = Identifier(
        value = s"n12345678${idSuffixes.head}",
        identifierType = IdentifierType.LCNames
      ),
      label = s"The concept of ${idSuffixes.head}",
      canonicalId = idSuffixes.map(suffix => s"abcdef$suffix"),
      ontologyType = Seq("Concept")
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

  Feature("Filtering single-id Concepts already in the index") {
    info(
      """
     | NotInIndexFlow consumes Concepts and only emits those that have not already been indexed.
     | It does this by checking for the Concept's canonicalId
      """.stripMargin
    )

    Scenario(s"No Concepts are already indexed") {
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

      val client = givenAnIndex(
        storedConcepts.map(concept =>
          s"""{"fields": {"canonicalId": ["${concept.canonicalId.head}"]}}"""
        )
      )

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
          .flatMap(_.canonicalId)
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

  Feature("Filtering multi-id Concepts already in the index") {
    info("""
        These scenarios are not currently expected to be exercised in "Real Life",
        a Concept that enters this flow is expected to have exactly
        one canonicalId, as it is only later (on DB insertion) that those ids 
        are merged.
        
        However, these tests demonstrate/prove the capacity that they could.
        For example, it may be beneficial to pre-merge the records in a given
        batch to reduce the size of the bulk requests.
    """)
    Scenario(
      "Concepts that have some ids yet to be stored are not filtered out"
    ) {
      Given("two concepts, each with two ids")
      val concepts =
        Seq(givenAMultiIdConcept(101, 102), givenAMultiIdConcept(103, 104))

      And("an index with one of those ids in it")
      val client = givenAnIndex(
        s"""{"fields": {"canonicalId": ["${concepts.head.canonicalId.head}"]}}"""
      )

      When("NotInIndexFlow is run")
      val results = runFlow(concepts, client, 2)
      Then("both concepts are returned")
      results shouldBe concepts
    }

    Scenario("Concepts that have all of their ids stored are filtered out") {
      Given("two concepts, each with two ids")
      val concepts =
        Seq(givenAMultiIdConcept(101, 102), givenAMultiIdConcept(103, 104))

      And("an index with all of the ids of the first")
      val client = givenAnIndex(
        concepts.head.canonicalId.map(canonicalId =>
          s"""{"fields": {"canonicalId": ["$canonicalId"]}}"""
        )
      )

      When("NotInIndexFlow is run")
      val results = runFlow(concepts, client, 1)
      Then("only the second concept is returned")
      results.loneElement shouldBe concepts(1)
    }

  }
}
