import sbt._

object ExternalDependencies {
  val versions = new {
    val akka = "2.7.0"
    val akkaHttp =
      "10.2.9" // This is a separate library to the rest of the akka-* world
    val awsLambda = "1.2.3"
    val awsLambdaEvents = "3.11.0"
    val awsSdk = "2.17.271"
    val ficus = "1.5.2"
    val grizzledSlf4j = "1.3.4"
    val gson = "2.9.0"
    val logback = "1.4.0"
    val scalatest = "3.2.12"
    val typesafeConfig = "1.4.2"
    val uPickle = "2.0.0"
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
    "com.lihaoyi" %% "upickle" % versions.uPickle
  )

  val akkaDeps =
    Seq(akka.actorTyped, akka.stream, akka.http, akka.streamTestkit)

  val awsLambda = Seq(
    "com.amazonaws" % "aws-lambda-java-core" % versions.awsLambda,
    "com.google.code.gson" % "gson" % versions.gson
  )

  val awsLambdaEvents = Seq(
    "com.amazonaws" % "aws-lambda-java-events" % versions.awsLambdaEvents
  )

  val awsSecrets = Seq(
    "software.amazon.awssdk" % "secretsmanager" % versions.awsSdk,
    "software.amazon.awssdk" % "sts" % versions.awsSdk
  )

  val awsSns = Seq(
    "software.amazon.awssdk" % "sns" % versions.awsSdk,
    "software.amazon.awssdk" % "sts" % versions.awsSdk
  )
}

object ServiceDependencies {
  import ExternalDependencies._
  val common: Seq[ModuleID] =
    scalatest ++ logging ++ uPickle ++ akkaDeps ++ awsSecrets

  val ingestor: Seq[ModuleID] =
    scalatest ++
      logging ++
      config ++
      akkaDeps ++ awsLambda

  val aggregator: Seq[ModuleID] = {
    scalatest ++ logging ++ config ++ akkaDeps ++ awsLambda ++ awsLambdaEvents ++ awsSns
  }

  val recorder: Seq[ModuleID] = {
    scalatest ++ logging ++ config ++ awsLambda ++ awsLambdaEvents
  }
}
