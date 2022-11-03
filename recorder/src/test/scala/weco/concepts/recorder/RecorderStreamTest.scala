package weco.concepts.recorder

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.testkit.scaladsl.TestSink
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.concepts.common.elasticsearch.BulkUpdateResult
import weco.concepts.common.fixtures.TestElasticHttpClient
import weco.concepts.common.model.{
  AuthoritativeConcept,
  CatalogueConcept,
  Identifier,
  IdentifierType
}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class RecorderStreamTest
    extends AnyFunSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience {
  val authoritativeConceptsIndex = "authoritative-concepts-test"
  val catalogueConceptsIndex = "catalogue-concepts-test"
  var targetIndex = "concepts-test"

  it("indexes merged concepts from a given identifier Source") {
    implicit val actorSystem: ActorSystem = ActorSystem("test")
    val authoritativeConcepts = Seq(
      AuthoritativeConcept(
        identifier = Identifier(
          value = "sh95000541",
          identifierType = IdentifierType.LCSubjects
        ),
        label = "World Wide Web",
        alternativeLabels = Seq(
          "W3 (World Wide Web)",
          "WWW (World Wide Web)",
          "Web (World Wide Web)",
          "World Wide Web (Information retrieval system)"
        )
      ),
      AuthoritativeConcept(
        identifier = Identifier(
          value = "n83217500",
          identifierType = IdentifierType.LCNames
        ),
        label = "Wellcome, Henry S. (Henry Solomon), Sir, 1853-1936",
        alternativeLabels = Seq(
          "Wellcome, H. S. (Henry Solomon), Sir, 1853-1936",
          "Wellcome, Henry Solomon, Sir, 1853-1936",
          "Wellcome, Henry, Sir, 1853-1936"
        )
      )
    )
    val catalogueConcepts = Seq(
      CatalogueConcept(
        identifier = Identifier(
          value = "n83217500",
          identifierType = IdentifierType.LCNames
        ),
        label = "Henry Wellcome",
        canonicalId = "123abcde",
        ontologyType = "Person"
      ),
      CatalogueConcept(
        identifier = Identifier(
          value = "sh95000541",
          identifierType = IdentifierType.LCSubjects
        ),
        label = "The Internet",
        canonicalId = "123abcde",
        ontologyType = "Concept"
      ),
      CatalogueConcept(
        identifier = Identifier(
          value = "things",
          identifierType = IdentifierType.LabelDerived
        ),
        label = "Things",
        canonicalId = "123abcde",
        ontologyType = "Concept"
      )
    )
    val (stream, testClient) =
      testStream(authoritativeConcepts, catalogueConcepts)

    Source(catalogueConcepts.map(_.identifier.toString))
      .via(stream.recordIds)
      .runWith(TestSink[BulkUpdateResult]())
      .request(1)
      .expectNext(
        BulkUpdateResult(
          took = 1234L,
          errored = Map.empty,
          updated = catalogueConcepts.map(_.canonicalId),
          noop = Nil
        )
      )
      .expectComplete()

    testClient.requests.count(
      _.uri.path.toString() == s"/$authoritativeConceptsIndex/_mget"
    ) shouldBe 1
    testClient.requests.count(
      _.uri.path.toString() == s"/$catalogueConceptsIndex/_mget"
    ) shouldBe 1
    testClient.requests.count(_.uri.path.toString() == "/_bulk") shouldBe 1
  }

  it("indexes merged concepts from an entire source index") {
    implicit val actorSystem: ActorSystem = ActorSystem("test")
    def identifier(i: Int): Identifier = Identifier(
      value = f"sh$i%010d",
      identifierType = IdentifierType.LCSubjects
    )
    def canonicalId(i: Int): String = f"$i%08x"
    val nCatalogueConcepts = 5000
    val catalogueConcepts = (1 to nCatalogueConcepts).map(i =>
      CatalogueConcept(
        identifier = identifier(i),
        label = s"Catalogue concept $i",
        canonicalId = canonicalId(i),
        ontologyType = "Subject"
      )
    )
    val authoritativeConcepts = (1 to (0.8 * nCatalogueConcepts).toInt).map(i =>
      AuthoritativeConcept(
        identifier = identifier(i),
        label = s"Authoritative concept $i",
        alternativeLabels = Nil
      )
    )
    val (stream, _) = testStream(authoritativeConcepts, catalogueConcepts)

    val allResultsFuture = stream.recordAllCatalogueConcepts
      .runWith(Sink.seq)
    whenReady(allResultsFuture) { allResults =>
      allResults.flatMap(_.updated) shouldBe catalogueConcepts.map(
        _.canonicalId
      )
    }
  }

  def testStream(
    authoritativeConcepts: Seq[AuthoritativeConcept],
    catalogueConcepts: Seq[CatalogueConcept]
  )(implicit mat: Materializer): (RecorderStream, TestElasticHttpClient) = {
    import weco.concepts.common.fixtures.ElasticsearchResponses._
    implicit val ec: ExecutionContext = mat.executionContext
    def handleMget: PartialFunction[HttpRequest, Future[Try[HttpResponse]]] = {
      case HttpRequest(HttpMethods.GET, uri, _, entity, _)
          if uri.path.toString() == s"/$authoritativeConceptsIndex/_mget" =>
        mgetResponse(
          index = authoritativeConceptsIndex,
          existing = authoritativeConcepts,
          entity = entity
        )
      case HttpRequest(HttpMethods.GET, uri, _, entity, _)
          if uri.path.toString() == s"/$catalogueConceptsIndex/_mget" =>
        mgetResponse(
          index = catalogueConceptsIndex,
          existing = catalogueConcepts,
          entity = entity
        )
    }
    val client = new TestElasticHttpClient(
      handleBulkUpdate orElse
        handleMget orElse
        handlePitCreation orElse
        handleSearch(catalogueConcepts) orElse
        handlePitDeletion
    )
    new RecorderStream(
      authoritativeConceptsIndexName = authoritativeConceptsIndex,
      catalogueConceptsIndexName = catalogueConceptsIndex,
      targetIndexName = targetIndex,
      elasticHttpClient = client,
      maxRecordsPerBulkRequest = 10
    ) -> client
  }
}
