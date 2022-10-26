package weco.concepts.aggregator

import akka.Done
import akka.stream.scaladsl.{Flow, Keep, Sink}
import grizzled.slf4j.Logging
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.{
  PublishBatchRequest,
  PublishBatchRequestEntry
}

import scala.concurrent.Future
import scala.jdk.CollectionConverters._
import scala.compat.java8.FutureConverters._

class TopicPublisher(snsClient: SnsAsyncClient, topicArn: String)
    extends Logging {
  private val batchSize = 10
  private val concurrency = 10

  lazy private val topicName = topicArn.split(":").last

  def sink: Sink[String, Future[Done]] =
    Flow[String]
      .grouped(batchSize)
      .mapAsyncUnordered(concurrency) { messages =>
        snsClient
          .publishBatch(batchRequest(messages))
          .toScala
      }
      .map {
        case result if !result.failed().isEmpty =>
          val failures = result.failed().asScala.map(_.message())
          throw new RuntimeException(
            s"Failed to publish messages: ${failures.mkString(",")}"
          )
        case result =>
          info(
            s"Published ${result.successful().size()} messages to topic $topicName"
          )
          result
      }
      .toMat(Sink.ignore)(Keep.right)

  def batchRequest(messages: Seq[String]): PublishBatchRequest =
    PublishBatchRequest
      .builder()
      .topicArn(topicArn)
      .publishBatchRequestEntries(
        messages.zipWithIndex.map { case (message, index) =>
          PublishBatchRequestEntry
            .builder()
            .message(message)
            .id(index.toString)
            .build()
        }.asJavaCollection
      )
      .build()
}
