package com.rgl10.krapi

import java.util.UUID
import java.nio.channels.AsynchronousChannelGroup

import cats.effect.{ContextShift, IO, Timer}
import org.apache.kafka.common.serialization.Deserializer
import spinoco.fs2.kafka
import spinoco.fs2.kafka._
import spinoco.protocol.kafka._

class StreamingConsumer(kafkaBroker: HostAndPort)(
    implicit cs: ContextShift[IO],
    timer: Timer[IO],
    group: AsynchronousChannelGroup,
    logger: KafkaLogger
) {

  // TODO - deal with empty topics
  def streamTopic[K, V](
      topicName: String,
      partitions: Int
  )(implicit keyDeserializer: Deserializer[K], valueDeserializer: Deserializer[V]): RecordStream[K, V] =
    stream.flatMap { kc =>
      (0 until partitions).toList.map { p =>
        kc.subscribe(topic(topicName), partition(p), HeadOffset)
          .takeThrough(m => (m.offset + 1) < m.tail)
          .map(_.toRecord[K, V](topicName, p))
      }.reduce(_.merge(_))
    }

  private val stream = kafka.client[IO](
    Set(broker(kafkaBroker.host, kafkaBroker.port)),
    ProtocolVersion.Kafka_0_10_2,
    UUID.randomUUID().toString
  )
}
