package weco.concepts.aggregator

import grizzled.slf4j.Logging
import org.apache.http.HttpHost
import org.elasticsearch.client.{
  Request,
  Response,
  ResponseException,
  RestClient
}

import scala.util.{Failure, Success, Try}
import scala.io.Source

class Indexer(hostname: String, port: Int, scheme: String) extends Logging {
  val elasticClient = RestClient
    .builder(new HttpHost(hostname, port, scheme))
    .setCompressionEnabled(true)
    .build()

  def bulk(couplets: Seq[String]): Response = {
    val rq = new Request("post", "_bulk")
    rq.setJsonEntity(couplets.mkString(start = "", sep = "\n", end = "\n"))
    elasticClient.performRequest(rq)
  }

  def createIndex(indexName: String): Unit = {
    Try(elasticClient.performRequest(new Request("head", indexName))) match {
      case Success(_) => println(s"index $indexName already exists")
      case Failure(exception: ResponseException) =>
        if (exception.getResponse.getStatusLine.getStatusCode == 404) {
          val rq = new Request("put", indexName)
          rq.setJsonEntity(
            Source.fromResource("index.json").getLines().mkString("\n")
          )
          val response = elasticClient.performRequest(rq)
          info(response)
        }
      // Should not be possible to reach a different kind of exception here, so
      // if we do, make it the caller's problem
      case Failure(exception) => throw exception
    }
  }
  def close(): Unit = elasticClient.close()
}
