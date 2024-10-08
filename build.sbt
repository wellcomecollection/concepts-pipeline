import java.io.File
import java.util.UUID
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider
import sbtassembly.MergeStrategy

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

def jarMergeStrategy(
  oldStrategy: String => MergeStrategy
): String => MergeStrategy = {
  case PathList(ps @ _*) if ps.last == "module-info.class" =>
    // The module-info.class files in logback-classic and logback-core clash.
    MergeStrategy.rename
  case PathList(ps @ _*) if ps.last == "io.netty.versions.properties" =>
    // AWS libraries bring along this file.  They are all bodily the same, but with a
    // comment containing the time they were generated
    MergeStrategy.first
  case x =>
    // Do whatever the default is for this file.
    oldStrategy(x)
}

lazy val ingestor = setupProject(
  project,
  folder = "ingestor",
  localDependencies = Seq(common),
  externalDependencies = ServiceDependencies.ingestor
).settings(
  assembly / assemblyOutputPath := file("target/ingestor.jar"),
  assembly / mainClass := Some("weco.concepts.ingestor.Main"),
  assembly / assemblyMergeStrategy := jarMergeStrategy(
    (ThisBuild / assemblyMergeStrategy).value
  )
)

lazy val aggregator = setupProject(
  project,
  folder = "aggregator",
  localDependencies = Seq(common),
  externalDependencies = ServiceDependencies.aggregator
).settings(
  assembly / assemblyOutputPath := file("target/aggregator.jar"),
  assembly / mainClass := Some("weco.concepts.aggregator.Main"),
  assembly / assemblyMergeStrategy := jarMergeStrategy(
    (ThisBuild / assemblyMergeStrategy).value
  )
)

lazy val recorder = setupProject(
  project,
  folder = "recorder",
  localDependencies = Seq(common),
  externalDependencies = ServiceDependencies.recorder
).settings(
  assembly / assemblyOutputPath := file("target/recorder.jar"),
  assembly / mainClass := Some("weco.concepts.recorder.Main"),
  assembly / assemblyMergeStrategy := jarMergeStrategy(
    (ThisBuild / assemblyMergeStrategy).value
  )
)
