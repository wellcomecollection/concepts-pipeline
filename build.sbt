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

lazy val common = setupProject(
  project,
  "common",
  externalDependencies = ServiceDependencies.common
)

lazy val ingestor = setupProject(
  project,
  folder = "ingestor",
  localDependencies = Seq(common),
  externalDependencies = ServiceDependencies.ingestor
)

lazy val aggregator = setupProject(
  project,
  folder = "aggregator",
  localDependencies = Seq(common),
  externalDependencies = ServiceDependencies.aggregator
).settings(
  assembly / assemblyOutputPath := file("target/aggregator.jar"),
  assembly / mainClass := Some("weco.concepts.aggregator.Main"),
  assembly / assemblyMergeStrategy := {
    case PathList(ps @ _*) if ps.last == "module-info.class" =>
      // The module-info.class files in logback-classic and logback-core clash.
      MergeStrategy.rename
//    case PathList(ps @ _*) if ps.last == "Log4j2Plugins.dat" =>
//      MergeStrategy.discard
    case PathList(ps @ _*) if ps.last == "io.netty.versions.properties" =>
      MergeStrategy.rename
    case x =>
      // Do whatever the default is for this file.
      val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
      oldStrategy(x)
  }
)

lazy val aggregatorLambda = setupProject(
  project,
  folder = "aggregatorLambda",
  localDependencies = Nil,
  externalDependencies = ServiceDependencies.aggregatorLambda
).settings(
  assembly / assemblyOutputPath := file("target/aggregator_lambda.jar"),
  assembly / assemblyMergeStrategy := {
    case PathList(ps @ _*) if ps.last == "module-info.class" =>
      // The module-info.class files in logback-classic and logback-core clash.
      MergeStrategy.rename
    //    case PathList(ps @ _*) if ps.last == "Log4j2Plugins.dat" =>
    //      MergeStrategy.discard
    case PathList(ps @ _*) if ps.last == "io.netty.versions.properties" =>
      MergeStrategy.first
    case x =>
      // Do whatever the default is for this file.
      val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
      oldStrategy(x)
  }
)

// AWS Credentials to read from S3
s3CredentialsProvider := { _ =>
  val builder = new STSAssumeRoleSessionCredentialsProvider.Builder(
    "arn:aws:iam::760097843905:role/platform-ci",
    UUID.randomUUID().toString
  )
  builder.build()
}
