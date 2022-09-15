package weco.concepts.aggregator

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import grizzled.slf4j.Logging
import weco.concepts.aggregator.sources.WorksSnapshotSource

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
    val source = workId match {
      case "all" =>
        WorksSnapshotSource(
          maxFrameKiB,
          snapshotUrl
        )
      case _ => workIdSource(Array(workId).iterator)
    }
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
