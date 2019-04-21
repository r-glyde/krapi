package com.rgl10.krapi.config

import cats.data.Reader
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.{MatchesRegex, Url}
import eu.timepit.refined.W

// TODO - go straight to custom class for kafkaBroker
final case class KrapiConfig(kafkaBrokers: String Refined MatchesRegex[W.`"[a-zA-Z.]+:[0-9]+"`.T],
                             schemaRegistry: String Refined Url,
                             krapiPort: Int = 8080)

object KrapiConfig {
  implicit def configure: Reader[Config, KrapiConfig] = Reader(_.krapi)
}

final case class Config(krapi: KrapiConfig)
