name := "krapi"
version := "0.1"

lazy val root = Project(id = "krapi", base = file("."))
  .aggregate(common, core, cli)
  .enablePlugins(DockerComposePlugin)

lazy val common = module("common")
  .settings(resolvers += Resolver.sonatypeRepo("snapshots"))

lazy val core = module("core")
  .enablePlugins(JavaServerAppPackaging, DockerComposePlugin)
  .settings(resolvers ++= Seq(
      Resolver.sonatypeRepo("snapshots"),
      "confluent-release" at "http://packages.confluent.io/maven/"
    )
  )
  .dependsOn(common)

lazy val cli = module("krapi-cli")
  .enablePlugins(JavaServerAppPackaging)
  .settings(resolvers += Resolver.sonatypeRepo("snapshots"))
  .dependsOn(common)

def module(name: String): Project =
  Project(id = name, base = file(name))
    .settings(
      scalaVersion := "2.12.8",
      scalacOptions += "-Ypartial-unification",
    )
    .settings(DockerSettings.common)
