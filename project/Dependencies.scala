import sbt._

object ExternalDependencies {
  lazy val versions = new {
    val scalatest = "3.2.12"
  }

  val scalatest = Seq(
    "org.scalatest" %% "scalatest" % versions.scalatest % "test"
  )
}

object ServiceDependencies {
  val ingestor: Seq[ModuleID] = ExternalDependencies.scalatest
}
