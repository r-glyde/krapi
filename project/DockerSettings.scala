import com.typesafe.sbt.SbtNativePackager.autoImport.packageName
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._
import sbt.Keys._

object DockerSettings {

  val dockerRepo = "r-glyde"
  
  lazy val common = Seq(
    dockerBaseImage := "openjdk:8u201-alpine",
    dockerRepository := Some(dockerRepo),
    dockerLabels := Map("maintainer" -> "r-glyde"),
    dockerUpdateLatest := true,
    packageName in Docker := s"krapi/${name.value}"
  )
}