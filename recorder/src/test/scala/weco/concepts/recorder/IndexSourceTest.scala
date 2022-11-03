package weco.concepts.recorder

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.testkit.scaladsl.TestSink
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.concepts.common.fixtures.TestElasticHttpClient
import weco.concepts.common.json.Indexable
import weco.concepts.common.json.JsonOps._

import scala.concurrent.{ExecutionContext, Future}

class IndexSourceTest extends AnyFunSpec with Matchers with ScalaFutures {
  import weco.concepts.common.fixtures.ElasticsearchResponses._

  it("creates a point-in-time ID when initialised") {
    implicit val actorSystem: ActorSystem = ActorSystem("test")
    implicit val ec: ExecutionContext = actorSystem.dispatcher
    val testClient = new TestElasticHttpClient(handlePitCreation)

    IndexSource[TestDoc](elasticHttpClient = testClient, indexName = "test")
      .runWith(TestSink[TestDoc]())
      .request(1)
      .expectError()

    testClient.requests should contain(
      HttpRequest(
        method = HttpMethods.POST,
        uri = Uri("/test/_pit?keep_alive=1m")
      )
    )
  }

  it("returns a page of search results") {
    implicit val actorSystem: ActorSystem = ActorSystem("test")
    implicit val ec: ExecutionContext = actorSystem.dispatcher
    val testDocs = (1 to 1000).map(id => TestDoc(id.toString))
    val testClient =
      new TestElasticHttpClient(
        handlePitCreation orElse
          handleSearch(testDocs)
      )

    val results = IndexSource[TestDoc](
      elasticHttpClient = testClient,
      indexName = "test",
      pageSize = 100
    )
      .runWith(TestSink[TestDoc]())
      .request(100)
      .expectNextN(100)

    results shouldBe testDocs.slice(0, 100)
  }

  it("returns subsequent pages of search results") {
    implicit val actorSystem: ActorSystem = ActorSystem("test")
    implicit val ec: ExecutionContext = actorSystem.dispatcher
    val testDocs = (1 to 1000).map(id => TestDoc(id.toString))
    val testClient = new TestElasticHttpClient(
      handlePitCreation orElse
        handleSearch(testDocs)
    )

    val results = IndexSource[TestDoc](
      elasticHttpClient = testClient,
      indexName = "test",
      pageSize = 100
    )
      .runWith(TestSink[TestDoc]())
      .request(1000)
      .expectNextN(1000)

    results shouldBe testDocs
  }

  it("reuses its point-in-time ID") {
    implicit val actorSystem: ActorSystem = ActorSystem("test")
    implicit val ec: ExecutionContext = actorSystem.dispatcher
    val testDocs = (1 to 1000).map(id => TestDoc(id.toString))
    val testClient = new TestElasticHttpClient(
      handlePitCreation orElse
        handleSearch(testDocs)
    )

    IndexSource[TestDoc](
      elasticHttpClient = testClient,
      indexName = "test",
      pageSize = 100
    )
      .runWith(TestSink[TestDoc]())
      .request(1000)
      .expectNextN(1000)

    val usedPitIds = Future
      .sequence(testClient.requests.collect {
        case HttpRequest(HttpMethods.POST, uri, _, entity, _)
            if uri.path.endsWith("_search") =>
          Unmarshal(entity)
            .to[String]
            .map(ujson.read(_))
            .map(_.opt[String]("pit", "id"))
      })
      .map(_.flatten)

    whenReady(usedPitIds) { ids =>
      ids.toSet.size shouldBe 1
    }
  }

  it("replaces the point-in-time ID if a search updates it") {
    implicit val actorSystem: ActorSystem = ActorSystem("test")
    implicit val ec: ExecutionContext = actorSystem.dispatcher
    val testDocs = (1 to 200).map(id => TestDoc(id.toString))
    val testClient = new TestElasticHttpClient(
      handlePitCreation orElse
        handleSearch(testDocs, _.reverse) orElse
        handlePitDeletion
    )

    IndexSource[TestDoc](
      elasticHttpClient = testClient,
      indexName = "test",
      pageSize = 100
    )
      .runWith(TestSink[TestDoc]())
      .request(200)
      .expectNextN(200)

    val usedPitIds = Future
      .sequence(testClient.requests.collect {
        case HttpRequest(HttpMethods.POST, uri, _, entity, _)
            if uri.path.endsWith("_search") =>
          Unmarshal(entity)
            .to[String]
            .map(ujson.read(_))
            .map(_.opt[String]("pit", "id"))
      })
      .map(_.flatten)

    whenReady(usedPitIds) { ids =>
      ids.toSet.size shouldBe 2
    }
  }

  it(
    "deletes the point-in-time and closes itself when no more results are returned"
  ) {
    implicit val actorSystem: ActorSystem = ActorSystem("test")
    implicit val ec: ExecutionContext = actorSystem.dispatcher
    val testClient = new TestElasticHttpClient(
      handlePitCreation orElse
        handleSearch(Nil) orElse
        handlePitDeletion
    )

    IndexSource[TestDoc](
      elasticHttpClient = testClient,
      indexName = "test",
      pageSize = 100
    )
      .runWith(TestSink[TestDoc]())
      .ensureSubscription()
      .expectComplete()

    testClient.requests.find {
      case HttpRequest(HttpMethods.DELETE, uri, _, _, _)
          if uri.path.endsWith("_pit") =>
        true
      case _ => false
    } should not be None
  }

  case class TestDoc(id: String)
  implicit val indexableTestdoc: Indexable[TestDoc] = new Indexable[TestDoc] {
    def id(t: TestDoc): String = t.id

    def toDoc(t: TestDoc): ujson.Value = ujson.Obj("id" -> t.id)

    def fromDoc(doc: ujson.Value): Option[TestDoc] =
      doc.opt[String]("id").map(TestDoc)
  }
}
