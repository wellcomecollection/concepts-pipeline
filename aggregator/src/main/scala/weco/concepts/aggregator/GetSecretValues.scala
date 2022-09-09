package weco.concepts.aggregator

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest

object GetSecretValues {
  def apply(keys: Seq[String]): Map[String, String] = {
    val region = Region.EU_WEST_1
    val secretsClient: SecretsManagerClient = SecretsManagerClient.builder
      .region(region)
      .credentialsProvider(ProfileCredentialsProvider.create)
      .build
    val secrets = keys
      .map(secretName => secretName -> getValue(secretsClient, secretName))
      .toMap
    secretsClient.close()
    secrets
  }

  def getValue(
    secretsClient: SecretsManagerClient,
    secretName: String
  ): String = {
    val valueRequest = GetSecretValueRequest.builder.secretId(secretName).build
    val valueResponse = secretsClient.getSecretValue(valueRequest)
    valueResponse.secretString
  }
}
