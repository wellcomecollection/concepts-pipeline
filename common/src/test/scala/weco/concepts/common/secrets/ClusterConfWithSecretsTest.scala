package weco.concepts.common.secrets

import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers
import weco.concepts.common.elasticsearch.ElasticAkkaHttpClient.ClusterConfig

class ClusterConfWithSecretsTest
    extends AnyFeatureSpec
    with Matchers
    with GivenWhenThen {

  Feature("Resolving secret keys in ClusterConf") {
    def resolver(keys: Seq[String]): Map[String, String] = {
      keys.map(secretName => secretName -> secretName.reverse).toMap
    }

    val replacer = new ClusterConfWithSecrets(resolver)

    Scenario("Resolving secrets") {
      Given("a ClusterConfig instance with secrets")
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
        "the host and password secrets are resolved"
      )
      resolvedConfig should have(
        Symbol("host")("example.com"),
        Symbol("password")(Some("password"))
      )

      And("secret resolution on the new config should be off")
      resolvedConfig.resolveSecrets shouldBe false

      And(
        "the other properties are left alone"
      )
      resolvedConfig should have(
        Symbol("scheme")("gopher"),
        Symbol("port")(1234),
        Symbol("username")(Some("Henry"))
      )
    }

    Scenario("Disabling secret resolution") {
      Given("a ClusterConfig instance with secret resolution switched off")
      val config = ClusterConfig(
        scheme = "gopher",
        host = "moc.elpmaxe",
        port = 1234,
        username = Some("Henry"),
        password = Some("drowssap"),
        resolveSecrets = false
      )
      When("ClusterConfWithSecrets is applied")
      val resolvedConfig = replacer(config)
      Then("nothing happens")
      resolvedConfig shouldBe config
    }

    Scenario("Nothing to do") {
      Given("a ClusterConfig instance with no password, and resolveSecrets on")
      val config = ClusterConfig(
        scheme = "gopher",
        host = "example.com",
        port = 1234,
        username = Some("Henry"),
        password = None,
        resolveSecrets = true
      )
      When("ClusterConfWithSecrets is applied")
      val resolvedConfig = replacer(config)
      Then("nothing happens")
      resolvedConfig shouldBe config
    }
  }
}
