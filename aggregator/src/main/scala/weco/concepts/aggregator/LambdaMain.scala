package weco.concepts.aggregator

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import grizzled.slf4j.Logging

import java.util.{Map => JavaMap}
object LambdaMain
    extends RequestHandler[JavaMap[String, String], String]
    with Logging {
  override def handleRequest(
    event: JavaMap[String, String],
    context: Context
  ): String = {
    val workId = event.get("workId")
    info(s"running aggregator lambda for $workId")
    // TODO: Consider changing what is currently in Main to not be an App
    //  Then having a CLI Main and a Lambda Main, both of which look a
    //  bit like this object.
    //  As it is, Main is a bit overloaded when used this way.
    Main.main(if (workId.isEmpty) Array() else Array(workId))
    "Done"
  }
}
