package weco.concepts.aggregator

import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers
import weco.concepts.aggregator.secrets.ClusterConfWithSecrets
import weco.concepts.common.elasticsearch.ElasticAkkaHttpClient.ClusterConfig

class ClusterConfWithSecretsTest
    extends AnyFeatureSpec
    with Matchers
    with GivenWhenThen {
  Feature("Resolving secret keys in ClusterConf") {
    Given("a secret resolver that reverses the key")
    def resolver(keys: Seq[String]): Map[String, String] = {
      keys.map(secretName => secretName -> secretName.reverse).toMap
    }
    val replacer = new ClusterConfWithSecrets(resolver)
    And("a ClusterConfig instance with secrets")
    val config = ClusterConfig(
      scheme = "gopher",
      host = "moc.elpmaxe",
      port = 1234,
      username = Some("Henry"),
      password = Some("drowssap"),
      resolveSecrets = true
    )
    When("ClusterConfWithSecrets is applied")
    val resolvedConfig = replacer(config)
    Then(
      "the host and password secrets are resolved, leaving other value as they were"
    )
    resolvedConfig shouldBe ClusterConfig(
      scheme = "gopher",
      host = "example.com",
      port = 1234,
      username = Some("Henry"),
      password = Some("password")
    )
    And("secret resolution on the new config should be off")
    resolvedConfig.resolveSecrets shouldBe false
  }
}
