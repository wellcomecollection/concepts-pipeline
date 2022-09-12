package weco.concepts.aggregator

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest

/** Fetch secrets from AWS Secrets Manager.
  *
  * Ideally, this would have been provided environmentally, butAWS Lambda does
  * not provide a built-in way to provide secrets to the function being run.
  *
  * It is possible to provide environment variables, but they are not secure
  * enough for database keys, as they are visible in the Lambda GUI. Therefore,
  * we have to fetch them as part of the init phase of the function.
  *
  * code cribbed from this Java sample:
  * https://github.com/awsdocs/aws-doc-sdk-examples/blob/af08838cbf272adbeda96ebbf7c5bfe14f0db80d/javav2/example_code/secretsmanager/src/main/java/com/example/secrets/GetSecretValue.java
  */
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

  private def getValue(
    secretsClient: SecretsManagerClient,
    secretName: String
  ): String = {
    val valueRequest = GetSecretValueRequest.builder.secretId(secretName).build
    val valueResponse = secretsClient.getSecretValue(valueRequest)
    valueResponse.secretString
  }
}
