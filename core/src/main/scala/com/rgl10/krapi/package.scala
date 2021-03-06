package com.rgl10

import cats.effect.IO
import cats.syntax.option._
import com.rgl10.krapi.common._
import fs2.kafka._
import io.circe.Json
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import io.confluent.kafka.serializers.{AbstractKafkaAvroSerDeConfig, KafkaAvroDeserializer}
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.common.serialization.Deserializer
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder}
import pureconfig.{ConfigReader, ConvertHelpers}

import scala.collection.JavaConverters._

package object krapi {

  type RecordStream[K, V] = fs2.Stream[IO, Record[K, V]]

  implicit val hostAndPortReader =
    ConfigReader.fromString[HostAndPort](ConvertHelpers.catchReadError(HostAndPort(_).get))

  implicit def subscriptionDecoder: EntityDecoder[IO, SubscriptionDetails] = jsonOf[IO, SubscriptionDetails]

  implicit def recordEncoder: EntityEncoder[IO, Record[Json, Json]] = jsonEncoderOf[IO, Record[Json, Json]]

  def avroDeserializer(schemaRegistryUrl: String, isKey: Boolean): Deserializer[GenericRecord] = {
    val kafkaAvroDeserializer = new KafkaAvroDeserializer()
    kafkaAvroDeserializer.configure(Map {
      AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG -> schemaRegistryUrl
    }.asJava, isKey)
    kafkaAvroDeserializer.asInstanceOf[Deserializer[GenericRecord]]
  }

  implicit class ConsumerRecordOps(val cr: ConsumerRecord[Array[Byte], Array[Byte]]) extends AnyVal {
    def toJsonRecord[K, V](kD: Deserializer[K], vD: Deserializer[V]): Record[Json, Json] = {
      val encoded: String => Option[Json] = v => parse(v).fold(_ => v.asJson.some, _.some)
      Record(
        cr.topic,
        cr.serializedKeySize.flatMap(_ => encoded(kD.deserialize(cr.topic, cr.key).toString)),
        cr.serializedValueSize.flatMap(_ => encoded(vD.deserialize(cr.topic, cr.value).toString)),
        cr.partition,
        cr.timestamp.createTime.get
      )
    }
  }
}
