package weco.concepts.aggregatorLambda

import grizzled.slf4j.Logging
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
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
object GetSecretValues extends Logging {
  def apply(keys: Seq[String]): Map[String, String] = {
    val region = Region.EU_WEST_1
    info("creating credentialsProvider")
    val credentialsProvider = EnvironmentVariableCredentialsProvider.create()
    info("building secretsManager client")
    val secretsClient: SecretsManagerClient = SecretsManagerClient.builder
      .region(region)
      .credentialsProvider(credentialsProvider)
      .build()
    info("fetching secrets")
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
    info(s"fetching $secretName")
    val valueRequest = GetSecretValueRequest.builder.secretId(secretName).build
    val valueResponse = secretsClient.getSecretValue(valueRequest)
    valueResponse.secretString
  }
}
