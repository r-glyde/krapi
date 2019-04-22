package com.rgl10.krapi

import java.util.Date

import cats.Show
import cats.effect.IO
import cats.syntax.show._
import sourcecode.{File, Line}
import spinoco.fs2.log.{Detail, Log, LogContext}

import scala.util.{Failure, Success, Try}

class KafkaLogger extends Log[IO] {
  override def log(level: Log.Level.Value, message: => String, detail: => Detail, thrown: Option[Throwable])(
      implicit line: Line,
      file: File,
      ctx: LogContext): IO[Unit] =
    IO.delay {
      println(s"${new Date()} [KAFKA-LOGGER] ${level.toString.toUpperCase} $detail - $message")
      if (thrown.nonEmpty) thrown.get.printStackTrace()
    }

  override def observe[A](level: Log.Level.Value, message: => String, detail: => Detail)(
      f: IO[A])(implicit evidence$1: Show[A], line: Line, file: File, ctx: LogContext): IO[A] =
    f map { a =>
      Try(a) match {
        case Success(v) => info(v.show); v
        case Failure(e) => throw e
      }
    }

  override def observeRaised[A](level: Log.Level.Value, message: => String, detail: => Detail)(
      f: IO[A])(implicit line: Line, file: File, ctx: LogContext): IO[A] =
    f map { a =>
      Try(a) match {
        case Success(v) => v
        case Failure(e) => error(e.getMessage); throw e
      }
    }
}
