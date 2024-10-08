import sbt._

object ExternalDependencies {
  val versions = new {
    val pekko = "1.1.1"
    val pekkoHttp = "1.1.0"
    val awsLambda = "1.2.1"
    val awsLambdaEvents = "3.11.0"
    val awsSdk = "2.17.271"
    val ficus = "1.5.2"
    val grizzledSlf4j = "1.3.4"
    val gson = "2.9.1"
    val logback = "1.4.14"
    val scalatest = "3.2.19"
    val typesafeConfig = "1.4.3"
    val uPickle = "4.0.2"
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

  val pekko = new {
    val actorTyped = "org.apache.pekko" %% s"pekko-actor-typed" % versions.pekko
    val http = "org.apache.pekko" %% "pekko-http" % versions.pekkoHttp
    val stream = "org.apache.pekko" %% s"pekko-stream" % versions.pekko
    val streamTestkit =
      "org.apache.pekko" %% s"pekko-stream-testkit" % versions.pekko % Test
  }

  val uPickle = Seq(
    "com.lihaoyi" %% "upickle" % versions.uPickle
  )

  val pekkoDeps =
    Seq(pekko.actorTyped, pekko.stream, pekko.http, pekko.streamTestkit)

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
    scalatest ++ logging ++ uPickle ++ pekkoDeps ++ awsSecrets

  val ingestor: Seq[ModuleID] =
    scalatest ++
      logging ++
      config ++
      pekkoDeps ++ awsLambda

  val aggregator: Seq[ModuleID] = {
    scalatest ++ logging ++ config ++ pekkoDeps ++ awsLambda ++ awsLambdaEvents ++ awsSns
  }

  val recorder: Seq[ModuleID] = {
    scalatest ++ logging ++ config ++ awsLambda ++ awsLambdaEvents
  }
}
