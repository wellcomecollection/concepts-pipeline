package weco.concepts.common.aws

import grizzled.slf4j.Logging
import software.amazon.awssdk.auth.credentials.{
  AwsCredentialsProvider,
  DefaultCredentialsProvider,
  EnvironmentVariableCredentialsProvider
}
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder
import software.amazon.awssdk.regions.Region

object AuthenticatedClient extends Logging {
  private val region = Region.EU_WEST_1

  sealed trait CredentialsProvider
  object CredentialsProvider {
    case object Environment extends CredentialsProvider
    case object Default extends CredentialsProvider
  }

  def apply[Builder <: AwsClientBuilder[Builder, Client], Client](
    credentialsProvider: CredentialsProvider,
    clientBuilder: AwsClientBuilder[Builder, Client]
  ): Client =
    clientBuilder
      .region(region)
      .credentialsProvider(awsProvider(credentialsProvider))
      .build()

  private def awsProvider(
    credentialsProvider: CredentialsProvider
  ): AwsCredentialsProvider =
    credentialsProvider match {
      case CredentialsProvider.Environment =>
        EnvironmentVariableCredentialsProvider.create()
      case CredentialsProvider.Default =>
        DefaultCredentialsProvider.create()
    }
}
