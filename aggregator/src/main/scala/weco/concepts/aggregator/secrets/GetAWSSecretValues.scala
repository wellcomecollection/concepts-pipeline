package weco.concepts.aggregator.secrets

import grizzled.slf4j.Logging
import software.amazon.awssdk.auth.credentials.{
  AwsCredentialsProvider,
  DefaultCredentialsProvider,
  EnvironmentVariableCredentialsProvider
}
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
class GetAWSSecretValues(credentialsProvider: AwsCredentialsProvider)
    extends Logging {
  def apply(keys: Seq[String]): Map[String, String] = {
    val region = Region.EU_WEST_1
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

object GetAWSSecretValues extends Logging {
  // Choose a credentials provider for AWS to use to sign in in order to then
  // fetch secrets.
  // Do not use Default in production, it is very slow on Lambda.
  sealed trait CredentialsProvider
  case object Environment extends CredentialsProvider
  case object Default extends CredentialsProvider
  def apply(
    credentialsType: CredentialsProvider
  ): GetAWSSecretValues = {
    info("creating credentialsProvider")

    val credentialsProvider: AwsCredentialsProvider = credentialsType match {
      case Environment =>
        EnvironmentVariableCredentialsProvider.create()
      case Default =>
        DefaultCredentialsProvider.create()
    }

    new GetAWSSecretValues(credentialsProvider)
  }

}
