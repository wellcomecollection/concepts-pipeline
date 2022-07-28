import sbt._

object ExternalDependencies {
  val versions = new {
    val akka = "2.6.19"
    val ficus = "1.5.2"
    val grizzledSlf4j = "1.3.4"
    val logback = "1.2.11"
    val scalatest = "3.2.12"
    val typesafeConfig = "1.4.2"
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

  def akka(libraryNames: String*): Seq[ModuleID] = libraryNames.map(library =>
    "com.typesafe.akka" %% s"akka-$library" % versions.akka
  )
}

object ServiceDependencies {
  import ExternalDependencies._

  val ingestor: Seq[ModuleID] =
    scalatest ++ logging ++ config ++ akka("actor-typed", "stream")
}
