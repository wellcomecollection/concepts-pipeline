package weco.concepts.aggregator

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import grizzled.slf4j.Logging

object LambdaMain extends RequestHandler[String, String] with Logging {

  override def handleRequest(
    event: String,
    context: Context
  ): String = {
//    val l = context.getLogger
//    l.log(s"ENVIRONMENT VARIABLES: ${gson.toJson(System.getenv)}\n")
//    l.log(s"CONTEXT: ${gson.toJson(context)}\n")
//    l.log(s"EVENT: ${gson.toJson(event)}\n")
    event.split(' ')
    info(event)
    Main.main(event.split(' ').filter(_.nonEmpty))
    info("lambda out")
    "Done"
  }
}
