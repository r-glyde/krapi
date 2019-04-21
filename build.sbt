name := "krapi"
version := "0.1"
scalaVersion := "2.12.8"

lazy val root = Project(id = "krapi", base = file("."))
  .aggregate(common, core, cli)
  .enablePlugins(DockerComposePlugin)

lazy val common = Project(id = "common", base = file("common"))
  .settings(scalacOptions += "-Ypartial-unification")
  .settings(
    resolvers ++= Seq(
      Resolver.sonatypeRepo("snapshots")
    )
  )

lazy val core = Project(id = "core", base = file("core"))
  .settings(scalacOptions += "-Ypartial-unification")
  .enablePlugins(JavaServerAppPackaging, DockerComposePlugin)
  .settings(DockerSettings.common)
  .settings(
    resolvers ++= Seq(
      Resolver.sonatypeRepo("snapshots"),
      "confluent-release" at "http://packages.confluent.io/maven/"
    )
  )
  .dependsOn(common)

lazy val cli = Project(id = "krapi-cli", base = file("krapi-cli"))
  .settings(scalacOptions += "-Ypartial-unification")
  .enablePlugins(JavaServerAppPackaging)
  .settings(DockerSettings.common)
  .settings(
    resolvers ++= Seq(
      Resolver.sonatypeRepo("snapshots")
    )
  )
  .dependsOn(common)