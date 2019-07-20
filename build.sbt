name := "krapi"

lazy val root = Project(id = "krapi", base = file("."))
  .aggregate(core, cli)
  .enablePlugins(DockerComposePlugin)
  .disablePlugins(ReleasePlugin)

lazy val core = module("core")
  .enablePlugins(JavaServerAppPackaging, DockerComposePlugin)
  .settings(Release.releaseSettings)
  .settings(resolvers ++= Seq(
      Resolver.sonatypeRepo("snapshots"),
      "confluent-release" at "http://packages.confluent.io/maven/"
    )
  )

lazy val cli = module("cli")
  .enablePlugins(JavaServerAppPackaging)
  .settings(Release.releaseSettings)
  .settings(resolvers += Resolver.sonatypeRepo("snapshots"))
  .dependsOn(core)

def module(name: String): Project =
  Project(id = name, base = file(name))
    .settings(
      scalaVersion := "2.12.8",
      scalacOptions += "-Ypartial-unification",
    )
    .settings(DockerSettings.common)
