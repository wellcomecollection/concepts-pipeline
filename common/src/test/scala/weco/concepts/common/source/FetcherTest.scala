package weco.concepts.common.source

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.stream.scaladsl.{Flow, Source}
import akka.stream.testkit.scaladsl._
import akka.util.ByteString
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import scala.util.{Failure, Success, Try}

class FetcherTest extends AnyFunSpec with Matchers {
  it(
    "creates a source of byte strings from a URL that returns a success status"
  ) {
    implicit val actorSystem: ActorSystem = ActorSystem("test")
    val testUrl = "https://test.test/things.test"
    val testContentLength = 1024
    val testData = ByteString.fromInts(1 to testContentLength: _*)

    val fetcher = mockFetcher({
      case HttpRequest(HttpMethods.GET, uri, _, _, _)
          if uri.toString() == testUrl =>
        Success(
          HttpResponse(
            status = StatusCodes.OK,
            entity = HttpEntity.Default(
              contentType = ContentType.Binary(mediaType =
                MediaTypes.`application/octet-stream`
              ),
              contentLength = testContentLength,
              data = Source.single(testData)
            )
          )
        )
    })

    fetcher
      .fetchFromUrl(testUrl)
      .runWith(TestSink[ByteString]())
      .request(1)
      .expectNext(testData)
      .expectComplete()
  }

  it("creates a source of byte strings from a redirect location") {
    implicit val actorSystem: ActorSystem = ActorSystem("test")
    val testInitialUrl = "https://test.test/things.test"
    val testRedirectUrl = "https://redirected.test/actual.data"
    val testContentLength = 1024
    val testData = ByteString.fromInts(1 to testContentLength: _*)

    val fetcher = mockFetcher({
      case HttpRequest(HttpMethods.GET, uri, _, _, _)
          if uri.toString() == testInitialUrl =>
        Success(
          HttpResponse(
            status = StatusCodes.SeeOther,
            headers = Seq(headers.Location(Uri(testRedirectUrl)))
          )
        )
      case HttpRequest(HttpMethods.GET, uri, _, _, _)
          if uri.toString() == testRedirectUrl =>
        Success(
          HttpResponse(
            status = StatusCodes.OK,
            entity = HttpEntity.Default(
              contentType = ContentType.Binary(mediaType =
                MediaTypes.`application/octet-stream`
              ),
              contentLength = testContentLength,
              data = Source.single(testData)
            )
          )
        )
    })

    fetcher
      .fetchFromUrl(testInitialUrl)
      .runWith(TestSink[ByteString]())
      .request(1)
      .expectNext(testData)
      .expectComplete()
  }

  it("fails if a URL returns an unexpected status") {
    implicit val actorSystem: ActorSystem = ActorSystem("test")
    val testUrl = "https://test.test/things.test"

    val fetcher = mockFetcher({
      case HttpRequest(HttpMethods.GET, uri, _, _, _)
          if uri.toString() == testUrl =>
        Success(HttpResponse(status = StatusCodes.NotFound))
    })

    fetcher
      .fetchFromUrl(testUrl)
      .runWith(TestSink[ByteString]())
      .request(1)
      .expectError()
      .getMessage should include("Unexpected status")
  }

  it("fails if the connection fails") {
    implicit val actorSystem: ActorSystem = ActorSystem("test")
    val testUrl = "https://test.test/things.test"

    val fetcher = mockFetcher({
      case HttpRequest(HttpMethods.GET, uri, _, _, _)
          if uri.toString() == testUrl =>
        Failure(new RuntimeException("Bleep blorp BOOOM!"))
    })

    fetcher
      .fetchFromUrl(testUrl)
      .runWith(TestSink[ByteString]())
      .request(1)
      .expectError()
      .getMessage should include("Failure making request")
  }

  def mockFetcher(
    mapping: PartialFunction[HttpRequest, Try[HttpResponse]]
  ): Fetcher = new Fetcher(Flow.fromFunction { case (request, url) =>
    mapping.applyOrElse[HttpRequest, Try[HttpResponse]](
      request,
      unhandled => throw new RuntimeException(s"Unhandled request: $unhandled")
    ) -> url
  })
}
