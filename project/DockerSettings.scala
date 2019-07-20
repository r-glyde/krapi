import com.typesafe.sbt.SbtNativePackager.autoImport.packageName
import com.typesafe.sbt.packager.docker.Cmd
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._
import sbt.Keys._

object DockerSettings {
  
  lazy val common = Seq(
    dockerBaseImage := "openjdk:8u201-alpine",
    dockerRepository := Some("glyderj"),
    dockerLabels := Map("maintainer" -> "r-glyde"),
    dockerUpdateLatest := true,
    packageName in Docker := s"krapi-${name.value}",
    dockerCommands ++= Seq(
      Cmd("USER", "root"),
      Cmd("RUN", "apk update && apk add bash")
    )
  )
}