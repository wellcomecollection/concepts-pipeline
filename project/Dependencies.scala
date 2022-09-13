import sbt._

object ExternalDependencies {
  val versions = new {
    val akka = "2.6.19"
    val akkaHttp =
      "10.2.9" // This is a separate library to the rest of the akka-* world
    val ficus = "1.5.2"
    val grizzledSlf4j = "1.3.4"
    val logback = "1.4.0"
    val scalatest = "3.2.12"
    val typesafeConfig = "1.4.2"
    val elastic4s = "8.3.2"
  }

  val scalatest = Seq(
    "org.scalatest" %% "scalatest" % versions.scalatest % Test
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

  val akka = new {
    val actorTyped = "com.typesafe.akka" %% s"akka-actor-typed" % versions.akka
    val http = "com.typesafe.akka" %% "akka-http" % versions.akkaHttp
    val stream = "com.typesafe.akka" %% s"akka-stream" % versions.akka
    val streamTestkit =
      "com.typesafe.akka" %% s"akka-stream-testkit" % versions.akka % Test
  }

  val uPickle = Seq(
    "com.lihaoyi" %% "upickle" % "2.0.0"
  )

  val elasticsearchLowLevel = Seq(
    "org.elasticsearch.client" % "elasticsearch-rest-client" % "8.4.1"
  )

  val akkaDeps =
    Seq(akka.actorTyped, akka.stream, akka.http, akka.streamTestkit)

  val awsLambda = Seq(
    "com.amazonaws" % "aws-lambda-java-core" % "1.2.1"
//    "software.amazon.awssdk" % "lambda" % "2.17.271",
//    "com.amazonaws" % "aws-lambda-java-events" % "3.11.0",
//    "com.google.code.gson" % "gson" % "2.9.0",
//    "com.amazonaws" % "aws-lambda-java-log4j2" % "1.5.1"
//    "com.amazonaws" % "aws-lambda-java-log4j2" % "1.5.1",
  )

  val awsSecrets = Seq(
    "software.amazon.awssdk" % "secretsmanager" % "2.17.271",
    "software.amazon.awssdk" % "sts" % "2.17.271"
  )
//  val awsSecrets = Seq(
//    "com.amazonaws.secretsmanager" % "aws-secretsmanager-caching-java" % "1.0.1"
//  )
}

object ServiceDependencies {
  import ExternalDependencies._
  val common: Seq[ModuleID] =
    scalatest ++ logging ++ uPickle ++ akkaDeps

  val ingestor: Seq[ModuleID] =
    scalatest ++
      logging ++
      config ++
      Seq(akka.actorTyped, akka.stream, akka.http, akka.streamTestkit)

  val aggregator: Seq[ModuleID] = {
    scalatest ++ logging ++ config ++ akkaDeps ++ elasticsearchLowLevel ++ awsLambda ++ awsSecrets
  }
}
