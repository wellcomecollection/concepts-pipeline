package weco.concepts.aggregator

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import grizzled.slf4j.Logging
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage

import scala.jdk.CollectionConverters._
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

/*
 * A Lambda Request Handler that responds to SQS Events to run the aggregator
 * over a list of SNS messages from the Works Ingestor.
 *
 * An SQS Event contains a list of SQS Messages, which contain, in the body,
 * the message retrieved from the SNS topic (as a string of JSON)
 *
 * See: https://docs.aws.amazon.com/lambda/latest/dg/with-sqs.html
 */

object SQSMain
    extends RequestHandler[SQSEvent, String]
    with AggregatorMain
    with Logging {

  override def handleRequest(
    event: SQSEvent,
    context: Context
  ): String = {
    val recordList: List[SQSMessage] = event.getRecords.asScala.toList
    val workIds = recordList flatMap { message: SQSMessage =>
      ujson.read(message.getBody).obj.get("Message").toList
    } map (_.str)

    context.getLogger.log(
      s"running aggregator lambda over ${workIds.length} works: $workIds, Lambda request: ${context.getAwsRequestId}"
    )
    val f = aggregator
      .run(workIdSource(workIds))
      .recover(err => error(err.getMessage))
      .map(_ => ())

    // Wait here so that lambda can run correctly.
    // Without waiting here, handleRequest finishes immediately.
    // Locally, (in a lambda container), that results in
    // the Lambda Runtime Interface Emulator telling us it took no time at all
    // and then Akka starts doing all the work.
    // I don't know what will happen in real life, but I suspect
    // that Lambda will shutdown the container and nothing will get done.
    Await.result(f, 10.minutes)
    "Done"
    // Do not clean up or shutdown resources.
    // If the same container is invoked again, then the actorSystem and db connection
    // can be reused.  This speeds up the processing of subsequent calls
    // When the Lambda expires (in 5 minutes or so), the whole container
    // will be destroyed, so everything will be cleaned up anyway.
  }

}
