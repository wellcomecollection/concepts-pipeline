package weco.concepts.aggregator

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import grizzled.slf4j.Logging

import java.util.{Map => JavaMap}
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object LambdaMain
    extends RequestHandler[JavaMap[String, String], String]
    with AggregatorMain
    with Logging {

  override def handleRequest(
    event: JavaMap[String, String],
    context: Context
  ): String = {
    val workId = event.get("workId")

    context.getLogger.log(
      s"running aggregator lambda for $workId, Lambda request: ${context.getAwsRequestId}"
    )
    println(workId)
    info(workId)

    // TODO: This is the CLI Logic (minus StdIn), but this runs the risk
    //   of spawning a 5-minute lambda for every notification if there is
    //   an unexpected change to the notifying message format
    //   e.g. if a typo is introduced, e.g. {"wrokId":"deadbeef"} would cause it to
    //   run the full snapshot-based aggregation for every message.  Better make
    //   snapshot run explicit.
    //   It may be best to have two distinct lambdas for the purpose. That way,
    //   the big one can be provided with lots of memory and a long timeout, and
    //   the other one can be kept short and forgetful.
    // Fixed, but only by removing snapshot source.
    val source: Source[String, NotUsed] = workIdSource(Array(workId).iterator)
    val f = aggregator
      .run(source)
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
