package weco.concepts.aggregator.secrets

import grizzled.slf4j.Logging
import weco.concepts.aggregator.Indexer

/*
 * A Resolver for ClusterConfig secrets.
 * If a ClusterConfig contains _references to_ secret values,
 * this class replaces those references with the actual secrets.
 *
 * The caller must provide a resolver that can look up the secrets and
 * return them in a map.
 *
 */
private class ClusterConfWithSecrets(
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
        ).map { case (key: String, value: String) =>
          (key.split('/').last, value)
        }

        clusterConfig.copy(
          host = secrets("public_host"),
          password = secrets.get("password")
        )
      case _ =>
        info("no secrets to resolve")
        clusterConfig
    }
  }
}
