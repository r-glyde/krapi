package com.rgl10.krapi

import java.util.UUID

import cats.effect.{ContextShift, IO, Timer}
import com.rgl10.krapi.config.{KrapiConfig, SecurityConfig}
import fs2.kafka._
import org.apache.kafka.clients._
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.config.SslConfigs

class StreamingConsumer(config: KrapiConfig)(implicit cs: ContextShift[IO], timer: Timer[IO]) {
  import StreamingConsumer._

  def streamTopic(topic: String, partitions: Int): fs2.Stream[IO, ConsumerRecord[Array[Byte], Array[Byte]]] = {
    val settings =
      ConsumerSettings[IO, Array[Byte], Array[Byte]]
        .withBootstrapServers(config.kafkaBrokers.map(_.fullname).mkString(","))
        .withAutoOffsetReset(AutoOffsetReset.Earliest)
        .withGroupId(UUID.randomUUID().toString)
        .withSecurityConfig(config.securityConfig)

    val topicPartitions = (0 until partitions).map(new TopicPartition(topic, _)).toSet
    val consumer        = consumerStream(settings).evalTap(_.subscribeTo(topic))

    consumer
      .evalMap(_.endOffsets(topicPartitions))
      .map(_.map { case (p, o) => p.partition() -> o })
      .flatMap { endOffsets =>
        if (endOffsets.values.toSet == Set(0)) fs2.Stream.empty
        else
          consumer
            .flatMap(_.partitionedStream)
            .map { ps =>
              ps.map(_.record).takeThrough(cr => (cr.offset + 1) < endOffsets.getOrElse(cr.partition, 0L))
            }
            .take(partitions)
            .parJoinUnbounded
      }
  }
}

object StreamingConsumer {
  implicit class ConsumerSettingsOps(val settings: ConsumerSettings[IO, Array[Byte], Array[Byte]]) extends AnyVal {
    def withSecurityConfig(config: Option[SecurityConfig]): ConsumerSettings[IO, Array[Byte], Array[Byte]] =
      config.fold(settings) { sc =>
        settings
          .withProperties(
            CommonClientConfigs.SECURITY_PROTOCOL_CONFIG            -> sc.securityProtocol,
            SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG                 -> sc.keystoreLocation,
            SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG                 -> sc.keystorePassword,
            SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG               -> sc.truststoreLocation,
            SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG               -> sc.truststorePassword,
            SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG -> sc.endpointIdentificationAlgorithm
          )
      }
  }
}
