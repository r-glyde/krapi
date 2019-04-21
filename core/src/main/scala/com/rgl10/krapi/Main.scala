package com.rgl10.krapi

import com.rgl10.krapi.config.Config
import pureconfig.generic.auto._
import pureconfig.loadConfigOrThrow
import cats.effect._
import cats.implicits._
import org.http4s.implicits._
import org.http4s.server.blaze._
import org.http4s.server.Router
import eu.timepit.refined.pureconfig._
import org.http4s.server.middleware.GZip

object Main extends IOApp {

  val krapiConfig = loadConfigOrThrow[Config].krapi

  def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO]
      .bindHttp(krapiConfig.krapiPort, "localhost")
      .withHttpApp(Router("/api" -> GZip(new Api(krapiConfig).router)).orNotFound)
      .resource
      .use(_ => IO.never)
      .as(ExitCode.Success)
}
