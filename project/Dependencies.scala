import sbt._

object Dependencies {

  val http4sVersion     = "0.20.6"
  val circeVersion      = "0.11.1"
  val pureconfigVersion = "0.11.1"
  val refinedVersion    = "0.9.8"

  lazy val coreDeps = http4s ++ circe ++ logging ++ pureconfigDeps ++ refinedDeps ++ testDeps ++ Seq(
    "org.apache.kafka" %% "kafka"                % "2.3.0",
    "org.apache.kafka" % "kafka-clients"         % "2.3.0",
    "io.confluent"     % "kafka-avro-serializer" % "5.3.0",
    "com.ovoenergy"    %% "fs2-kafka"            % "0.20.0-M2",
    "org.typelevel"    %% "cats-core"            % "1.6.1"
  )

  lazy val cliDeps = http4s ++ circe ++ refinedDeps ++ Seq(
    "com.github.scopt" %% "scopt"    % "4.0.0-RC2",
    "org.slf4j"        % "slf4j-nop" % "1.7.26"
  )

  private val http4s = Seq(
    "org.http4s" %% "http4s-dsl"          % http4sVersion,
    "org.http4s" %% "http4s-blaze-server" % http4sVersion,
    "org.http4s" %% "http4s-blaze-client" % http4sVersion,
    "org.http4s" %% "http4s-circe"        % http4sVersion
  )

  private val circe = Seq(
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-literal" % circeVersion,
    "io.circe" %% "circe-parser"  % circeVersion
  )

  private val logging = Seq(
    "com.typesafe.scala-logging" %% "scala-logging"   % "3.9.2",
    "org.slf4j"                  % "log4j-over-slf4j" % "1.7.26",
    "org.slf4j"                  % "slf4j-api"        % "1.7.26",
    "ch.qos.logback"             % "logback-classic"  % "1.2.3" % Runtime
  )

  private val pureconfigDeps = Seq(
    "com.github.pureconfig" %% "pureconfig"      % pureconfigVersion,
    "com.github.pureconfig" %% "pureconfig-cats" % pureconfigVersion
  )

  private val refinedDeps = Seq(
    "eu.timepit" %% "refined"            % refinedVersion,
    "eu.timepit" %% "refined-pureconfig" % refinedVersion,
    "eu.timepit" %% "refined-scalacheck" % refinedVersion,
    "eu.timepit" %% "refined-scopt"      % refinedVersion
  )

  private val testDeps = Seq(
    "org.scalatest"           %% "scalatest"                      % "3.0.8"  % Test,
    "org.scalacheck"          %% "scalacheck"                     % "1.14.0" % Test,
    "com.ironcorelabs"        %% "cats-scalatest"                 % "2.4.1"  % Test,
    "io.github.embeddedkafka" %% "embedded-kafka"                 % "2.3.0"  % Test,
    "com.danielasfregola"     %% "random-data-generator-magnolia" % "2.6"    % Test,
    "com.47deg"               %% "scalacheck-toolbox-datetime"    % "0.2.5"  % Test
  )
}
