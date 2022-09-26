package weco.concepts.aggregator.secrets

import grizzled.slf4j.Logging
import weco.concepts.common.elasticsearch.ElasticAkkaHttpClient.ClusterConfig

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
    clusterConfig: ClusterConfig
  ): ClusterConfig = {
    clusterConfig match {
      // The two parameters that are expected to contain references to secrets
      // are host and password.  This means that if resolveSecrets is true,
      // *both* of them must be secret keys, and not contain the actual value to be used.
      // If you try to put a secret key in any of the other properties, it won't resolve.
      // This is sufficient for the Aggregator application, but if this code is to be reused
      // elsewhere, then it will require a bit of a change to make it more general-purpose.
      case ClusterConfig(host, _, _, _, Some(password), true) =>
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
