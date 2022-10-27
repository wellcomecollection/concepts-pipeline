package weco.concepts.recorder

import akka.stream.scaladsl.{Sink, Source}
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import grizzled.slf4j.Logging
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage
import weco.concepts.common.elasticsearch.BulkUpdateResult

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters._

/*
 * A Lambda Request Handler that responds to SQS Events to run the Recorder
 * over a list of SNS messages from the Aggregator
 *
 * An SQS Event contains a list of SQS Messages, which contain, in the body,
 * the message retrieved from the SNS topic (as a string of JSON)
 *
 * See: https://docs.aws.amazon.com/lambda/latest/dg/with-sqs.html
 */

object SQSMain
    extends RequestHandler[SQSEvent, String]
    with Logging
    with RecorderMain {

  override def handleRequest(
    event: SQSEvent,
    context: Context
  ): String = {
    val recordList: List[SQSMessage] = event.getRecords.asScala.toList
    val conceptIds = recordList.map(_.getBody)

    info(
      s"Running recorder lambda over ${conceptIds.length} concepts: $conceptIds, Lambda request: ${context.getAwsRequestId}"
    )

    val f = indices.create(targetIndex).flatMap { done =>
      Source(conceptIds)
        .via(recorderStream.recordIds)
        .runWith(Sink.fold[Long, BulkUpdateResult](0L)(_ + _.updated.size))
        .map { total =>
          info(s"Recorded $total concepts")
          done
        }
    }

    // Wait here so that lambda can run correctly.
    // Without waiting here, handleRequest finishes immediately.
    // Locally, (in a lambda container), that results in
    // the Lambda Runtime Interface Emulator telling us it took no time at all
    // and then Akka starts doing all the work.
    // I don't know what will happen in real life, but I suspect
    // that Lambda will shutdown the container and nothing will get done.
    Await.result(f, 10 minutes)
    "Done"
    // Do not clean up or shutdown resources.
    // If the same container is invoked again, then the actorSystem and db connection
    // can be reused.  This speeds up the processing of subsequent calls
    // When the Lambda expires (in 5 minutes or so), the whole container
    // will be destroyed, so everything will be cleaned up anyway.
  }

}
