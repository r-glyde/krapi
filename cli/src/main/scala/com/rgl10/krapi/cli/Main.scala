package com.rgl10.krapi.cli

import java.util.concurrent.Executors

import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.implicits._
import com.monovore.decline._
import com.rgl10.krapi.cli.Config._
import com.rgl10.krapi.cli.Mode._
import com.rgl10.krapi.common._
import fs2.compress.gunzip
import fs2.io.stdout
import fs2.text.utf8Encode
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import jawnfs2._
import org.http4s.circe._
import org.http4s.client._
import org.http4s.client.blaze._
import org.http4s.client.dsl.io._
import org.http4s.dsl.io._
import org.http4s.{Header, Uri}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.ExecutionContext.fromExecutorService

object Main extends IOApp {
  val cliApp: Client[IO] => Command[CliApp] = client =>
    cliCommand {
      (urlOpt, modeOpt, entityTypeOpt, entityNameOpt, keyDeserializerOpt, valueDeserializerOpt, prettyPrintOpt).mapN {
        case (_, Metadata, None, _, _, _, _) =>
          ConfigurationError("Must provide type of metadata to describe via --entityType").asLeft
        case (_, Consumer, _, None, _, _, _) =>
          ConfigurationError("Topic to consume from must be provided via --entityName").asLeft
        case (url, Metadata, Some(entityType), maybeName, _, _, _) =>
          val baseUrl = Uri.unsafeFromString(url.value) / "api" / "metadata" / entityType.toString.toLowerCase
          client.expect[Json](maybeName.fold(baseUrl)(name => baseUrl / name)).map(println).asRight
        case (url, Consumer, _, Some(topic), keyD, valD, pretty) =>
          implicit val f = io.circe.jawn.CirceSupportParser.facade
          val requestBody = POST(
            SubscriptionDetails(topic, keyD.asString, valD.asString).asJson,
            Uri.unsafeFromString(url.value) / "api" / "consumer"
          )

          Resource
            .make(IO.delay(Executors.newFixedThreadPool(1)))(pool => IO.delay(pool.shutdown()))
            .use { blockingExecutor =>
              fs2.Stream
                .eval(requestBody)
                .flatMap { request =>
                  client
                    .stream(request.putHeaders(Header("Accept-Encoding", "gzip")))
                    .flatMap { response =>
                      response.body
                        .through(gunzip[IO](1024))
                        .chunks
                        .parseJsonStream
                        .map(maybePretty(_, pretty))
                    }
                }
                .intersperse("\n")
                .through(utf8Encode andThen stdout(fromExecutorService(blockingExecutor)))
                .compile
                .drain
            }
            .asRight
      }
  }

  def maybePretty(json: Json, pretty: Boolean): String = if (pretty) json.spaces2 else json.noSpaces

  def run(args: List[String]): IO[ExitCode] =
    BlazeClientBuilder[IO](global).resource.use { client =>
      IO {
        val command = cliApp(client)
        command
          .parse(args)
          .fold(
            h => {
              println(h)
              ExitCode.Error
            }, {
              case Left(error) =>
                println(error.errorMsg)
                println(command.showHelp)
                ExitCode.Error
              case Right(programme) =>
                programme.unsafeRunSync()
                ExitCode.Success
            }
          )
      }
    }
}
