package weco.concepts.common.elasticsearch

import grizzled.slf4j.Logging
import org.apache.http.HttpHost
import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.apache.http.impl.nio.reactor.IOReactorConfig
import org.elasticsearch.client.RestClientBuilder.HttpClientConfigCallback
import org.elasticsearch.client.{
  Request,
  Response,
  ResponseException,
  RestClient
}

import scala.util.{Failure, Success, Try}
import scala.io.Source
/*
 * Thin wrapper around the Elasticsearch Low-level Rest Client.
 * providing only what this pipeline needs.
 *
 * Other Elasticsearch Client implementations, such as elastic4s or
 * the ES High-level Java client bring along extra dependencies
 * and provide a lot more than we actually require.
 */
class Indexer(elasticClient: RestClient) extends Logging {

  def bulk(couplets: Seq[String]): Try[Response] = {
    val rq = new Request("post", "/_bulk")
    rq.setJsonEntity(couplets.mkString(start = "", sep = "\n", end = "\n"))
    Try(elasticClient.performRequest(rq))
  }

  def createIndex(indexName: String, indexConfig: String): Unit = {
    Try(
      elasticClient.performRequest(new Request("head", s"/$indexName"))
    ) match {
      case Success(_) =>
        info(s"index $indexName already exists, no need to create")
      case Failure(exception: ResponseException)
          if exception.getResponse.getStatusLine.getStatusCode == 404 =>
        val rq = new Request("put", s"/$indexName")
        rq.setJsonEntity(indexConfig)
        val response = elasticClient.performRequest(rq)
        info(response)
      // Not expected to reach a different kind of exception here,
      // unless something really exceptional has happened so
      // if we do, make it the caller's problem
      case Failure(exception) => throw exception
    }
  }
  def createIndex(indexName: String): Unit = createIndex(
    indexName = indexName,
    indexConfig = Source.fromResource("index.json").getLines().mkString("\n")
  )

  def close(): Unit = {
    info("closing ES Client")
    elasticClient.close()
  }
}

object Indexer {
  case class ClusterConfig(
    host: String,
    port: Int,
    scheme: String,
    username: Option[String],
    password: Option[String],
    resolveSecrets: Boolean = false
  )

  def apply(
    clusterConfig: ClusterConfig
  ): Indexer = {
    clusterConfig match {
      case ClusterConfig(host, port, scheme, _, _, _) =>
        new Indexer(
          RestClient
            .builder(new HttpHost(host, port, scheme))
            .setCompressionEnabled(true)
            .setHttpClientConfigCallback(
              clientConfigCallback(clusterConfig)
            )
            .build()
        )
    }
  }

  private def clientConfigCallback(
    clusterConfig: ClusterConfig
  ): HttpClientConfigCallback = {
    val addAuthentication: HttpClientConfigCallback = clusterConfig match {
      case ClusterConfig(_, _, _, Some(username), Some(password), _) =>
        val credentials = new UsernamePasswordCredentials(username, password)
        val credentialsProvider = new BasicCredentialsProvider()
        credentialsProvider.setCredentials(AuthScope.ANY, credentials)
        _.setDefaultCredentialsProvider(credentialsProvider)
      case _ => identity[HttpAsyncClientBuilder](_)
    }
    val addKeepAlive: HttpClientConfigCallback = _.setDefaultIOReactorConfig(
      IOReactorConfig
        .custom()
        .setSoKeepAlive(true)
        .build()
    )

    (addAuthentication.customizeHttpClient compose addKeepAlive.customizeHttpClient)(
      _
    )
  }
}
