package weco.concepts.aggregator

import grizzled.slf4j.Logging

/*
 * A Resolver for ClusterConfig secrets that fetches them from AWS Secrets Manager.
 *
 */
private object ClusterSecrets extends Logging {
  def apply(
    clusterConfig: Indexer.ClusterConfig
  ): Indexer.ClusterConfig = {
    clusterConfig match {
      case Indexer.ClusterConfig(host, _, _, _, Some(password), true) =>
        info("resolving cluster config secrets")
        val secrets = GetSecretValues(
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
