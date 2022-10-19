package weco.concepts.recorder

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import grizzled.slf4j.Logging
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage

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

object SQSMain extends RequestHandler[SQSEvent, String] with Logging {

  override def handleRequest(
    event: SQSEvent,
    context: Context
  ): String = {
    val recordList: List[SQSMessage] = event.getRecords.asScala.toList
    val conceptIds = recordList flatMap { message: SQSMessage =>
      ujson.read(message.getBody).obj.get("Message").toList
    } map (_.str)

    context.getLogger.log(
      s"running recorder lambda over ${conceptIds.length} concepts: $conceptIds, Lambda request: ${context.getAwsRequestId}"
    )
    "Done"
  }

}
