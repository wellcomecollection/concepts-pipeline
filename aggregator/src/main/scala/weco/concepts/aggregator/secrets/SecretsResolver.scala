package weco.concepts.aggregator.secrets

import grizzled.slf4j.Logging

/*
 * Selector to govern how to resolve secrets in config files.
 * Do not use AWSDefault in production, as it can be very slow.
 * When running on Lambda, use AWSEnvironment
 */
object SecretsResolver extends Logging {

  def apply(key: String): Seq[String] => Map[String, String] = {
    key match {
      case "AWSEnvironment" =>
        GetAWSSecretValues(GetAWSSecretValues.Environment).apply
      case "AWSDefault" => GetAWSSecretValues(GetAWSSecretValues.Default).apply
      case "None"       => keys => keys.map(key => key -> key).toMap
      case _ =>
        error(s"unknown secrets resolver key")
        keys => keys.map(key => key -> key).toMap
    }

  }

}
