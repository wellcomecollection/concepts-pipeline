package weco.concepts.recorder

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.http.scaladsl.unmarshalling.Unmarshal
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.testkit.scaladsl.TestSink
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
    val testClient = indexSourceTestClient()

    IndexSource[TestDoc](elasticHttpClient = testClient, indexName = "test")
      .runWith(TestSink[TestDoc]())
      .ensureSubscription()
      .expectComplete()

    testClient.requests.headOption should contain(
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
    val testClient = indexSourceTestClient(testDocs)

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
    val testClient = indexSourceTestClient(testDocs)

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
    val testClient = indexSourceTestClient(testDocs)

    IndexSource[TestDoc](
      elasticHttpClient = testClient,
      indexName = "test",
      pageSize = 100
    )
      .runWith(TestSink[TestDoc]())
      .request(1000)
      .expectNextN(1000)

    val usedPitIds = getPitsUsedInSearch(testClient.requests)
    whenReady(usedPitIds) { ids =>
      ids.toSet.size shouldBe 1
      testClient.requests
        .map(_.method)
        .count(_ == HttpMethods.DELETE) shouldBe 1
      testClient.requests.map(_.method).lastOption should contain(
        HttpMethods.DELETE
      )
    }
  }

  it("replaces the point-in-time ID if a search updates it") {
    implicit val actorSystem: ActorSystem = ActorSystem("test")
    implicit val ec: ExecutionContext = actorSystem.dispatcher
    val testDocs = (1 to 200).map(id => TestDoc(id.toString))
    val testClient = indexSourceTestClient(testDocs, _.reverse)

    IndexSource[TestDoc](
      elasticHttpClient = testClient,
      indexName = "test",
      pageSize = 100
    )
      .runWith(TestSink[TestDoc]())
      .request(200)
      .expectNextN(200)

    val usedPitIds = getPitsUsedInSearch(testClient.requests)
    whenReady(usedPitIds) { ids =>
      ids.toSet.size shouldBe 2
    }
  }

  it(
    "deletes the point-in-time and closes itself when no more results are returned"
  ) {
    implicit val actorSystem: ActorSystem = ActorSystem("test")
    implicit val ec: ExecutionContext = actorSystem.dispatcher
    val testClient = indexSourceTestClient()

    IndexSource[TestDoc](
      elasticHttpClient = testClient,
      indexName = "test",
      pageSize = 100
    )
      .runWith(TestSink[TestDoc]())
      .ensureSubscription()
      .expectComplete()

    val pitsFuture = getPitsUsedInSearch(testClient.requests)
    whenReady(pitsFuture) { pits =>
      val pit = pits.headOption
      pit should not be empty
      testClient.requests.lastOption should contain(
        HttpRequest(
          method = HttpMethods.DELETE,
          uri = Uri("/_pit"),
          entity = HttpEntity(
            ContentTypes.`application/json`,
            ujson.write(ujson.Obj("id" -> pit.get))
          )
        )
      )
    }
  }

  case class TestDoc(id: String)
  implicit val indexableTestdoc: Indexable[TestDoc] = new Indexable[TestDoc] {
    def id(t: TestDoc): String = t.id

    def toDoc(t: TestDoc): ujson.Value = ujson.Obj("id" -> t.id)

    def fromDoc(doc: ujson.Value): Option[TestDoc] =
      doc.opt[String]("id").map(TestDoc)
  }

  def getPitsUsedInSearch(
    requests: Seq[HttpRequest]
  )(implicit ec: ExecutionContext, mat: Materializer): Future[Seq[String]] =
    Future
      .sequence(requests.collect {
        case HttpRequest(HttpMethods.POST, uri, _, entity, _)
            if uri.path.endsWith("_search") =>
          Unmarshal(entity)
            .to[String]
            .map(ujson.read(_))
            .map(_.opt[String]("pit", "id"))
      })
      .map(_.flatten)
  def indexSourceTestClient(
    records: Seq[TestDoc] = Nil,
    transformPit: String => String = identity
  )(implicit ec: ExecutionContext, mat: Materializer): TestElasticHttpClient =
    new TestElasticHttpClient(
      handlePitCreation orElse
        handleSearch(records, transformPit) orElse
        handlePitDeletion
    )
}
