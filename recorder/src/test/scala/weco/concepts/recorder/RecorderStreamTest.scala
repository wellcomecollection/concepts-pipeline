package weco.concepts.recorder

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.concepts.common.elasticsearch.BulkUpdateResult
import weco.concepts.common.fixtures.TestElasticHttpClient
import weco.concepts.common.json.Indexable
import weco.concepts.common.json.Indexable.IndexableOps
import weco.concepts.common.model.{
  AuthoritativeConcept,
  Identifier,
  IdentifierType,
  UsedConcept
}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

class RecorderStreamTest extends AnyFunSpec with Matchers {
  val authoritativeConceptsIndex = "authoritative-concepts-test"
  val usedConceptsIndex = "used-concepts-test"
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
    val usedConcepts = Seq(
      UsedConcept(
        identifier = Identifier(
          value = "n83217500",
          identifierType = IdentifierType.LCNames
        ),
        label = "Henry Wellcome",
        canonicalId = "123abcde",
        ontologyType = "Person"
      ),
      UsedConcept(
        identifier = Identifier(
          value = "sh95000541",
          identifierType = IdentifierType.LCSubjects
        ),
        label = "The Internet",
        canonicalId = "123abcde",
        ontologyType = "Concept"
      ),
      UsedConcept(
        identifier = Identifier(
          value = "things",
          identifierType = IdentifierType.LabelDerived
        ),
        label = "Things",
        canonicalId = "123abcde",
        ontologyType = "Concept"
      )
    )
    val (stream, testClient) = testStream(authoritativeConcepts, usedConcepts)

    Source(usedConcepts.map(_.identifier.toString))
      .via(stream.recordIds)
      .runWith(TestSink[BulkUpdateResult]())
      .request(1)
      .expectNext(
        BulkUpdateResult(
          took = 1234L,
          errored = Map.empty,
          updated = usedConcepts.map(_.canonicalId),
          noop = Nil
        )
      )
      .expectComplete()

    testClient.requests.count(
      _.uri.path.toString() == s"/$authoritativeConceptsIndex/_mget"
    ) shouldBe 1
    testClient.requests.count(
      _.uri.path.toString() == s"/$usedConceptsIndex/_mget"
    ) shouldBe 1
    testClient.requests.count(_.uri.path.toString() == "/_bulk") shouldBe 1
  }

  def testStream(
    authoritativeConcepts: Seq[AuthoritativeConcept],
    usedConcepts: Seq[UsedConcept]
  )(implicit mat: Materializer): (RecorderStream, TestElasticHttpClient) = {
    implicit val ec: ExecutionContext = mat.executionContext
    def mgetResponse[T: Indexable](
      index: String,
      existing: Seq[T],
      entity: HttpEntity
    ): Future[Try[HttpResponse]] =
      Unmarshal(entity).to[String].map(ujson.read(_)).map { requestBody =>
        val requestedIds = requestBody("ids").arr.map(_.str)
        val existingMap = existing.map(doc => doc.id -> doc).toMap
        val docs = requestedIds
          .map(id =>
            existingMap
              .get(id)
              .map(indexable =>
                ujson.Obj(
                  "_index" -> index,
                  "_id" -> id,
                  "_version" -> 1,
                  "found" -> true,
                  "_source" -> indexable.toDoc
                )
              )
              .getOrElse(
                ujson.Obj("_index" -> index, "_id" -> id, "found" -> false)
              )
          )
        Success(
          HttpResponse(
            StatusCodes.OK,
            entity = HttpEntity(
              ContentTypes.`application/json`,
              ujson.write(ujson.Obj("docs" -> docs))
            )
          )
        )
      }
    val client = new TestElasticHttpClient(
      TestElasticHttpClient.defaultBulkHandler orElse {
        case HttpRequest(HttpMethods.GET, uri, _, entity, _)
            if uri.path.toString() == s"/$authoritativeConceptsIndex/_mget" =>
          mgetResponse(
            index = authoritativeConceptsIndex,
            existing = authoritativeConcepts,
            entity = entity
          )
        case HttpRequest(HttpMethods.GET, uri, _, entity, _)
            if uri.path.toString() == s"/$usedConceptsIndex/_mget" =>
          mgetResponse(
            index = usedConceptsIndex,
            existing = usedConcepts,
            entity = entity
          )
      }
    )
    new RecorderStream(
      authoritativeConceptsIndexName = authoritativeConceptsIndex,
      usedConceptsIndexName = usedConceptsIndex,
      targetIndexName = targetIndex,
      elasticHttpClient = client,
      maxRecordsPerBulkRequest = 10
    ) -> client
  }
}
