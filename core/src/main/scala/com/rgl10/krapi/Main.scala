package com.rgl10.krapi

import java.nio.channels.AsynchronousChannelGroup
import java.util.concurrent.Executors

import cats.effect._
import cats.implicits._
import com.rgl10.krapi.config.Config
import eu.timepit.refined.pureconfig._
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze._
import org.http4s.server.middleware.GZip
import pureconfig.generic.auto._
import pureconfig.loadConfigOrThrow

object Main extends IOApp {

  lazy val executor                            = Executors.newFixedThreadPool(10)
  implicit val group: AsynchronousChannelGroup = AsynchronousChannelGroup.withThreadPool(executor)

  val krapiConfig = loadConfigOrThrow[Config].krapi

  def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO]
      .bindHttp(krapiConfig.krapiPort, "localhost")
      .withHttpApp(Router("/api" -> GZip(new Api(krapiConfig).router)).orNotFound)
      .resource
      .use(_ => IO.never)
      .as(ExitCode.Success)
}
