package weco.concepts.aggregatorLambda

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import grizzled.slf4j.Logging
import java.util.{Map => JavaMap}

object LambdaHW
    extends RequestHandler[JavaMap[String, String], String]
    with Logging {
  private def getThing: Int = {
    info("hello from init")
    1
  }
  private final val numOfThings: Int = getThing
  private final val nameOfThings: String = {
    info("hello name")
    "My Name"
  }
  lazy val banana: Int = {
    info("banana")
    2
  }
  private final val secrets = GetSecretValues(
    List("elasticsearch/concepts-2022-08-31/public_host")
  )
  override def handleRequest(
    event: JavaMap[String, String],
    context: Context
  ): String = {
    debug("hello")
    info("hello")
    warn("hello")
    error("hello")
    info(nameOfThings)
    info(numOfThings)
    info(banana)
    info(secrets.isEmpty)
    info(secrets.values.head.slice(10, 20))

    val workId = event.get("workId")
    info(
      s"running aggregator lambda for $workId, Lambda request: ${context.getAwsRequestId}"
    )
    "Thanks."
  }
}
