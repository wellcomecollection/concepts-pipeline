package weco.concepts.aggregator

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.{
  BatchResultErrorEntry,
  PublishBatchRequest,
  PublishBatchResponse,
  PublishBatchResultEntry
}

import java.util.concurrent.CompletableFuture
import scala.collection.mutable
import scala.jdk.CollectionConverters._
import scala.compat.java8.FutureConverters._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class TopicPublisherTest extends AnyFunSpec with Matchers {
  implicit val actorSystem: ActorSystem = ActorSystem("test")

  it("publishes messages") {
    val messages = (1 to 100).map(i => s"message $i")
    val testClient = new TestSnsAsyncClient()
    val testPublisher = new TopicPublisher(
      snsClient = testClient,
      topicArn = "test"
    )
    Await.result(Source(messages).runWith(testPublisher.sink), Duration.Inf)

    testClient.publishedMessages should contain theSameElementsAs messages
  }

  it("throws if any messages fail") {
    val messages = (1 to 100).map(i => s"message $i")
    val testClient = new TestSnsAsyncClient(message => !message.contains("42"))
    val testPublisher = new TopicPublisher(
      snsClient = testClient,
      topicArn = "test"
    )

    val thrown = the[RuntimeException] thrownBy Await.result(
      Source(messages).runWith(testPublisher.sink),
      Duration.Inf
    )
    thrown.getMessage should include("message 42")
  }

  class TestSnsAsyncClient(
    messageSucceeds: String => Boolean = Function.const(true)
  ) extends SnsAsyncClient {
    private val publishedMessagesBuffer = mutable.Buffer.empty[String]
    def publishedMessages: Seq[String] = publishedMessagesBuffer.toSeq

    def serviceName(): String = "sns"
    def close(): Unit = ()

    override def publishBatch(
      publishBatchRequest: PublishBatchRequest
    ): CompletableFuture[PublishBatchResponse] = {
      val (successes, failures) = publishBatchRequest
        .publishBatchRequestEntries()
        .asScala
        .map(_.message())
        .zipWithIndex
        .partition { case (message, _) =>
          messageSucceeds(message)
        }
      publishedMessagesBuffer.appendAll(successes.map(_._1))
      Future
        .successful(
          PublishBatchResponse
            .builder()
            .failed(
              failures.map { case (message, id) =>
                BatchResultErrorEntry
                  .builder()
                  .id(id.toString)
                  .message(message)
                  .build()
              }.asJavaCollection
            )
            .successful(
              successes.map { case (_, id) =>
                PublishBatchResultEntry.builder().id(id.toString).build()
              }.asJavaCollection
            )
            .build()
        )
        .toJava
        .toCompletableFuture
    }
  }
}
