package com.rgl10.krapi.config

import cats.data.Reader
import com.rgl10.krapi.HostAndPort
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Url

final case class KrapiConfig(kafkaBrokers: List[HostAndPort],
                             schemaRegistry: String Refined Url,
                             port: Int = 8080,
                             securityConfig: Option[SecurityConfig])

object KrapiConfig {
  implicit def configure: Reader[Config, KrapiConfig] = Reader(_.krapi)
}

final case class Config(krapi: KrapiConfig)

final case class SecurityConfig(securityProtocol: String,
                                keystoreLocation: String,
                                keystorePassword: String,
                                truststoreLocation: String,
                                truststorePassword: String,
                                endpointIdentificationAlgorithm: String)
