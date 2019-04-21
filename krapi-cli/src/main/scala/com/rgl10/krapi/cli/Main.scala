package com.rgl10.krapi.cli

import java.util.concurrent.Executors

import cats.effect.{ExitCode, IO, IOApp}
import com.rgl10.krapi.SubscriptionDetails
import fs2.io.stdout
import fs2.text.utf8Encode
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import jawnfs2._
import org.http4s.Uri
import org.http4s.circe._
import org.http4s.client._
import org.http4s.client.blaze._
import org.http4s.client.dsl.io._
import org.http4s.dsl.io._
import scopt.OParser

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    BlazeClientBuilder[IO](global).resource
      .use(runClient(_, args))

  def blockingEcStream: fs2.Stream[IO, ExecutionContext] =
    fs2.Stream
      .bracket(IO.delay(Executors.newFixedThreadPool(4)))(pool => IO.delay(pool.shutdown()))
      .map(ExecutionContext.fromExecutorService)

  def maybePrettyPrint(prettyPrint: Boolean): Json => String =
    json => if (prettyPrint) s"${json.spaces2}," else s"${json.noSpaces},"

  def runClient(client: Client[IO], args: List[String]): IO[ExitCode] = IO {
    OParser.parse(Config.parser, args, Config()) match {
      case Some(Config(url, Metadata, entityType, entityName, _, _, _)) =>
        val metadataUrl = Uri.unsafeFromString(url) / "api" / "metadata" / entityType.toString.toLowerCase
        if (entityName.length > 0) println(client.expect[Json](metadataUrl / entityName).unsafeRunSync())
        else println(client.expect[Json](metadataUrl).unsafeRunSync())
        ExitCode.Success
      case Some(Config(url, Consumer, _, topic, keyD, valD, pretty)) if topic.length > 0 =>
        implicit val f = io.circe.jawn.CirceSupportParser.facade
        val req = POST(SubscriptionDetails(topic, keyD.toValue, valD.toValue).asJson,
                       Uri.unsafeFromString(url) / "api" / "consumer")
        val s = for {
          sr  <- fs2.Stream.eval(req)
          res <- client.stream(sr).flatMap(_.body.chunks.parseJsonStream)
        } yield res
        println("[")
        blockingEcStream.flatMap { blockingEC =>
          s.map(maybePrettyPrint(pretty)).intersperse("\n").through(utf8Encode).through(stdout(blockingEC))
        }.compile.drain.unsafeRunSync()
        println("\n]")
        ExitCode.Success
      case _ =>
        println("Try running with --help to display usage information")
        ExitCode.Error
    }
  }
}
