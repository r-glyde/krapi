package com.rgl10

import java.lang.{Long => JLong}

import cats.effect.IO
import cats.syntax.option._
import fs2.kafka._
import io.circe.Encoder
import io.circe.generic.auto._
import io.confluent.kafka.serializers.{AbstractKafkaAvroSerDeConfig, KafkaAvroDeserializer}
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.common.serialization.Deserializer
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import pureconfig.{ConfigReader, ConvertHelpers}

import scala.collection.JavaConverters._

package object krapi {

  type RecordStream          = fs2.Stream[IO, Record[String, String]]
  type SupportedDeserializer = Deserializer[_ >: GenericRecord with JLong with String <: Object]

  implicit val hostAndPortReader =
    ConfigReader.fromString[HostAndPort](ConvertHelpers.catchReadError(HostAndPort(_).get))

  implicit def subscriptionDecoder                     = jsonOf[IO, SubscriptionDetails]
  implicit def recordEncoder[K : Encoder, V : Encoder] = jsonEncoderOf[IO, Record[K, V]]
  implicit def genericRecordEncoder: Encoder[GenericRecord] =
    genericRecord => Encoder.encodeString(genericRecord.toString)

  def avroDeserializer(schemaRegistryUrl: String, isKey: Boolean): Deserializer[GenericRecord] = {
    val kafkaAvroSerDeConfig = Map[String, Any] {
      AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG -> schemaRegistryUrl
    }
    val kafkaAvroDeserializer = new KafkaAvroDeserializer()
    kafkaAvroDeserializer.configure(kafkaAvroSerDeConfig.asJava, isKey)
    kafkaAvroDeserializer.asInstanceOf[Deserializer[GenericRecord]]
  }

  implicit class ConsumerRecordOps(val cr: ConsumerRecord[Array[Byte], Array[Byte]]) extends AnyVal {
    def toRecord(keyDeserializer: SupportedDeserializer,
                 valueDeserializer: SupportedDeserializer): Record[String, String] =
      Record(
        cr.topic,
        if (cr.serializedKeySize.isDefined) keyDeserializer.deserialize(cr.topic, cr.key).toString.some else None,
        if (cr.serializedValueSize.isDefined) valueDeserializer.deserialize(cr.topic, cr.value).toString.some else None,
        cr.partition,
        cr.timestamp.createTime.get
      )
  }
}
