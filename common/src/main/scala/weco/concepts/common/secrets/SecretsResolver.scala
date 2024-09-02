package weco.concepts.common.secrets

import grizzled.slf4j.Logging
import weco.concepts.common.aws.AuthenticatedClient

/*
 * Selector to govern how to resolve secrets in config files.
 * Do not use AWSDefault in production, as it can be very slow.
 * When running on Lambda, use AWSEnvironment
 */
object SecretsResolver extends Logging {

  def apply(key: String): Seq[String] => Map[String, String] = {
    key match {
      case "AWSEnvironment" =>
        new GetAWSSecretValues(AuthenticatedClient.CredentialsProvider.Environment).apply
      case "AWSDefault" =>
        new GetAWSSecretValues(
          AuthenticatedClient.CredentialsProvider.Default
        ).apply
      case "None" => keys => keys.map(key => key -> key).toMap
      case _ =>
        error(s"unknown secrets resolver key")
        keys => keys.map(key => key -> key).toMap
    }

  }

}
