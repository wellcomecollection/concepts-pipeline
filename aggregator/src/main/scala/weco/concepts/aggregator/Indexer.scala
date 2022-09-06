package weco.concepts.aggregator

import grizzled.slf4j.Logging
import org.apache.http.HttpHost
import org.elasticsearch.client.{
  Request,
  Response,
  ResponseException,
  RestClient
}
import ujson.Value

import scala.util.{Failure, Success, Try}
import scala.io.Source
trait IndexerResponse {
  def body: Value.Value
}
class ElasticResponse(response: Response) extends IndexerResponse {
  def body: Value.Value = ujson.read(response.getEntity.getContent)
}

class LoggingResponse(ids: Seq[String]) extends IndexerResponse {
  def body: Value.Value = {
    val items = ids
      .map(id => s"""{"_id": "$id", "update": {"result": "printed"}}""")
      .mkString(",")
    println(s"""{"items": [$items]}""")
    ujson.read(s"""{"items": [$items]}""")
  }

}
trait Indexer {
  def bulk(couplets: Seq[String]): IndexerResponse
  def createIndex(indexName: String): Unit
  def close(): Unit
}

class LoggingIndexer extends Indexer with Logging {
  def bulk(couplets: Seq[String]): IndexerResponse = {
    val ids = couplets map { couplet =>
      ujson.read(couplet.split("\n")(0)).obj("update").obj("_id").value.toString
    }
    info(ids)
    new LoggingResponse(ids)
  }

  def createIndex(indexName: String): Unit = ()

  def close(): Unit = ()

}
/*
 * Thin wrapper around the Elasticsearch Low-level Rest Client.
 * providing only what this pipeline needs.
 *
 * Other Elasticsearch Client implementations, such as elastic4s or
 * the ES High-level Java client bring along extra dependencies
 * and provide a lot more than we actually require.
 */
class ElasticIndexer(elasticClient: RestClient) extends Indexer with Logging {

  def bulk(couplets: Seq[String]): IndexerResponse = {
    val rq = new Request("post", "_bulk")
    rq.setJsonEntity(couplets.mkString(start = "", sep = "\n", end = "\n"))
    new ElasticResponse(elasticClient.performRequest(rq))
  }

  def createIndex(indexName: String): Unit = {
    Try(elasticClient.performRequest(new Request("head", indexName))) match {
      case Success(_) =>
        info(s"index $indexName already exists, no need to create")
      case Failure(exception: ResponseException) =>
        if (exception.getResponse.getStatusLine.getStatusCode == 404) {
          val rq = new Request("put", indexName)
          rq.setJsonEntity(
            Source.fromResource("index.json").getLines().mkString("\n")
          )
          val response = elasticClient.performRequest(rq)
          info(response)
        }
      // Should not be possible to reach a different kind of exception here,
      // unless something really exceptional has happened so
      // if we do, make it the caller's problem
      case Failure(exception) => throw exception
    }
  }
  def close(): Unit = {
    info("closing ES Client")
    elasticClient.close()
  }
}

object ElasticIndexer {
  def apply(hostname: String, port: Int, scheme: String) =
    new ElasticIndexer(
      RestClient
        .builder(new HttpHost(hostname, port, scheme))
        .setCompressionEnabled(true)
        .build()
    )
}
