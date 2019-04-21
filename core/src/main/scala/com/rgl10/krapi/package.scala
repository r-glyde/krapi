package com.rgl10

import cats.effect.IO
import cats.syntax.option._
import io.circe.Encoder
import io.circe.generic.auto._
import io.confluent.kafka.serializers.{AbstractKafkaAvroSerDeConfig, KafkaAvroDeserializer}
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.common.serialization.{Deserializer, LongDeserializer, StringDeserializer}
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import pureconfig.{ConfigReader, ConvertHelpers}
import spinoco.fs2.kafka.TopicMessage

import scala.collection.JavaConverters._
import scala.util.{Success, Try}

package object krapi {

  implicit val hostAndPortReader = ConfigReader.fromString[HostAndPort](ConvertHelpers.catchReadError(HostAndPort(_)))

  implicit val stringDeser = new StringDeserializer()
  implicit val longDeser   = new LongDeserializer()

  implicit def subscriptionDecoder = jsonOf[IO, SubscriptionDetails]
  implicit def recordEncoder[K: Encoder, V: Encoder] = jsonEncoderOf[IO, Record[K, V]]
  implicit def genericRecordEncoder: Encoder[GenericRecord] = genericRecord => Encoder.encodeString(genericRecord.toString)

  def toAvroDeserializer(schemaRegistryUrl: String, isKey: Boolean): Deserializer[GenericRecord] = {
    val kafkaAvroSerDeConfig = Map[String, Any] {
      AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG -> schemaRegistryUrl
    }
    val kafkaAvroDeserializer = new KafkaAvroDeserializer()
    kafkaAvroDeserializer.configure(kafkaAvroSerDeConfig.asJava, isKey)
    kafkaAvroDeserializer.asInstanceOf[Deserializer[GenericRecord]]
  }

  implicit class TopicMessageOps(val m: TopicMessage) extends AnyVal {
    def asRecord[K, V](topic: String, partition: Int)(implicit keyDeserializer: Deserializer[K],
                                                      valueDeserializer: Deserializer[V]): Record[K, V] = {
      val key   = Try(keyDeserializer.deserialize(topic, m.key.toArray))
      val value = Try(valueDeserializer.deserialize(topic, m.message.toArray))
      (key, value) match {
        case (Success(k), Success(v)) if m.message.size > 0 => Record(topic, k, v.some, m.key.size, m.message.size, partition)
        case (Success(k), _) if m.message.size == 0 => Record(topic, k, none[V], m.key.size, -1L, partition)
      }
    }
  }
}
