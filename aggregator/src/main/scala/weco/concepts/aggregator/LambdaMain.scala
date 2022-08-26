package weco.concepts.aggregator

import java.util.{Map => JavaMap}
import com.google.gson.GsonBuilder
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import grizzled.slf4j.Logging

object LambdaMain
    extends RequestHandler[JavaMap[String, String], String]
    with Logging {
  val gson = new GsonBuilder().setPrettyPrinting.create

  override def handleRequest(
    event: JavaMap[String, String],
    context: Context
  ): String = {
    val l = context.getLogger
    l.log(s"ENVIRONMENT VARIABLES: ${gson.toJson(System.getenv)}\n")
    l.log(s"CONTEXT: ${gson.toJson(context)}\n")
    l.log(s"EVENT: ${gson.toJson(event)}\n")
    warn("hello")
    Main.main(Array("uk4kymkq", "yn8nshmc"))
    "Done"
  }
}
