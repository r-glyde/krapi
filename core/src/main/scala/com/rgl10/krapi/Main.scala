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

object Main extends IOApp {

  val krapiConfig = loadConfigOrThrow[Config].krapi

  // TODO - switch to gzip compression
  def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO]
      .bindHttp(8080, "localhost")
      .withHttpApp(Router("/api" -> new Api(krapiConfig).router).orNotFound)
      .resource
      .use(_ => IO.never)
      .as(ExitCode.Success)
}
