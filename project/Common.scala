import sbt.Keys._
import sbt._

object Common {
  val projectScalaVersion = "2.13.15"

  val settings: Seq[Def.Setting[_]] = Seq(
    scalaVersion := projectScalaVersion,
    organization := "weco",
    resolvers ++= Seq(
      "Wellcome releases" at "s3://releases.mvn-repo.wellcomecollection.org/"
    ),
    scalacOptions ++= Seq(
      "-deprecation",
      "-unchecked",
      "-encoding",
      "UTF-8",
      "-Xlint",
      "-Xverify",
      "-Xfatal-warnings",
      "-feature",
      "-language:postfixOps",
      "-Xcheckinit"
    ),
    updateOptions := updateOptions.value.withCachedResolution(true),
    Test / parallelExecution := false,
    // Don't build scaladocs
    // https://www.scala-sbt.org/sbt-native-packager/formats/universal.html#skip-packagedoc-task-on-stage
    Compile / packageDoc / mappings := Nil
  )
}
