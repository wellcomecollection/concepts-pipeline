package weco.concepts.aggregator.secrets

import grizzled.slf4j.Logging
import weco.concepts.common.elasticsearch.Indexer

/*
 * A Resolver for ClusterConfig secrets.
 * If a ClusterConfig contains _references to_ secret values,
 * this class replaces those references with the actual secrets.
 *
 * The caller must provide a resolver that can look up the secrets and
 * return them in a map.
 *
 */
class ClusterConfWithSecrets(
  resolver: Seq[String] => Map[String, String]
) extends Logging {
  def apply(
    clusterConfig: Indexer.ClusterConfig
  ): Indexer.ClusterConfig = {
    clusterConfig match {
      case Indexer.ClusterConfig(host, _, _, _, Some(password), true) =>
        info("resolving cluster config secrets")
        val secrets = resolver(
          Seq(
            host,
            password
          )
        )
        clusterConfig.copy(
          host = secrets(host),
          password = secrets.get(password)
        )
      case _ =>
        info("no secrets to resolve")
        clusterConfig
    }
  }
}
