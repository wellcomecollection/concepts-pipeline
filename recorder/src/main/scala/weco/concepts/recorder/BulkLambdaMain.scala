package weco.concepts.recorder

import org.apache.pekko.stream.scaladsl.Sink
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import grizzled.slf4j.Logging
import weco.concepts.common.elasticsearch.BulkUpdateResult

import java.util.{Map => JavaMap}
import scala.concurrent.Await
import scala.concurrent.duration._

object BulkLambdaMain
    extends RequestHandler[JavaMap[String, String], String]
    with RecorderMain
    with Logging {

  def handleRequest(
    input: JavaMap[String, String],
    context: Context
  ): String = {
    info("Running recorder in bulk mode")

    val f = indices.create(targetIndex).flatMap { done =>
      recorderStream.recordAllCatalogueConcepts
        .runWith(Sink.fold[Long, BulkUpdateResult](0L)(_ + _.updated.size))
        .map { total =>
          info(s"Recorded $total concepts")
          done
        }
    }

    Await.result(f, 10 minutes)
    "Done"
  }

}
