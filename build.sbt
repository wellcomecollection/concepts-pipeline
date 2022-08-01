import java.io.File
import java.util.UUID
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider

def setupProject(
  project: Project,
  folder: String,
  localDependencies: Seq[Project] = Seq(),
  externalDependencies: Seq[ModuleID] = Seq()
): Project = {
  val dependsOn = localDependencies
    .map { project: Project =>
      ClasspathDependency(
        project = project,
        configuration = Some("compile->compile;test->test")
      )
    }

  project
    .in(new File(folder))
    .settings(Common.settings: _*)
    .enablePlugins(JavaAppPackaging)
    .dependsOn(dependsOn: _*)
    .settings(libraryDependencies ++= externalDependencies)
}

lazy val ingestor = setupProject(
  project,
  folder = "ingestor",
  externalDependencies = ServiceDependencies.ingestor
)

lazy val aggregator = setupProject(
  project,
  folder = "aggregator",
  externalDependencies = ServiceDependencies.aggregator
)

// AWS Credentials to read from S3
s3CredentialsProvider := { _ =>
  val builder = new STSAssumeRoleSessionCredentialsProvider.Builder(
    "arn:aws:iam::760097843905:role/platform-ci",
    UUID.randomUUID().toString
  )
  builder.build()
}
