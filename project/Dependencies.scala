import sbt._

object ExternalDependencies {
  lazy val versions = new {
    val scalatest = "3.2.12"
    val grizzledSlf4j = "1.3.4"
    val logback = "1.2.11"
    val typesafeConfig = "1.4.2"
    val ficus = "1.5.2"
  }

  val scalatest = Seq(
    "org.scalatest" %% "scalatest" % versions.scalatest % "test"
  )

  val logging = Seq(
    "org.clapper" %% "grizzled-slf4j" % versions.grizzledSlf4j,
    "ch.qos.logback" % "logback-access" % versions.logback,
    "ch.qos.logback" % "logback-classic" % versions.logback,
    "ch.qos.logback" % "logback-core" % versions.logback
  )

  val config = Seq(
    "com.typesafe" % "config" % versions.typesafeConfig,
    "com.iheart" %% "ficus" % versions.ficus
  )
}

object ServiceDependencies {
  import ExternalDependencies._

  val ingestor: Seq[ModuleID] = scalatest ++ logging ++ config
  val aggregator: Seq[ModuleID] = scalatest ++ logging ++ config
}
