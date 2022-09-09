package weco.concepts.aggregator

import grizzled.slf4j.Logging

trait ClusterEnvConfig extends Logging {

  protected val resolveClusterSecrets: Boolean

  protected def getIndexer(clusterConfig: Indexer.ClusterConfig): Indexer = {
    Indexer(withClusterSecrets(clusterConfig))
  }

  private def withClusterSecrets(
    clusterConfig: Indexer.ClusterConfig
  ): Indexer.ClusterConfig = {
    if (resolveClusterSecrets) {

      val secrets = GetSecretValues(
        Seq(
          clusterConfig.host,
          clusterConfig.password.get
        )
      ).map { case (key: String, value: String) =>
        (key.split('/').last, value)
      }

      clusterConfig.copy(
        host = secrets("public_host"),
        password = secrets.get("password")
      )
    } else clusterConfig
  }
}
