package com.rgl10

import java.lang.{Long => JLong}

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
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.common.config.ConfigResource
import org.apache.kafka.common.serialization.Deserializer
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder}
import pureconfig.{ConfigReader, ConvertHelpers}

import scala.collection.JavaConverters._

package object krapi {

  type RecordStream[K, V]    = fs2.Stream[IO, Record[K, V]]
  type SupportedDeserializer = Deserializer[_ >: GenericRecord with JLong with String <: Object]

  implicit val hostAndPortReader =
    ConfigReader.fromString[HostAndPort](ConvertHelpers.catchReadError(HostAndPort(_).get))

  implicit def subscriptionDecoder: EntityDecoder[IO, SubscriptionDetails] = jsonOf[IO, SubscriptionDetails]
  implicit def recordEncoder: EntityEncoder[IO, Record[Json, Json]]        = jsonEncoderOf[IO, Record[Json, Json]]

  def avroDeserializer(schemaRegistryUrl: String, isKey: Boolean): Deserializer[GenericRecord] = {
    val kafkaAvroSerDeConfig = Map[String, Any] {
      AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG -> schemaRegistryUrl
    }
    val kafkaAvroDeserializer = new KafkaAvroDeserializer()
    kafkaAvroDeserializer.configure(kafkaAvroSerDeConfig.asJava, isKey)
    kafkaAvroDeserializer.asInstanceOf[Deserializer[GenericRecord]]
  }

  def encode(value: String): Option[Json] = parse(value).fold(_ => value.asJson.some, _.some)

  implicit class ConsumerRecordOps(val cr: ConsumerRecord[Array[Byte], Array[Byte]]) extends AnyVal {
    def toRecord(kD: SupportedDeserializer, vD: SupportedDeserializer): Record[Json, Json] =
      Record(
        cr.topic,
        if (cr.serializedKeySize.isDefined) encode(kD.deserialize(cr.topic, cr.key).toString) else none[Json],
        if (cr.serializedValueSize.isDefined) encode(vD.deserialize(cr.topic, cr.value).toString) else none[Json],
        cr.partition,
        cr.timestamp.createTime.get
      )
  }

  implicit class AdminClientOps(val ac: AdminClient) extends AnyVal {
    def listTopics: Set[String] = ac.listTopics().names().get().asScala.toSet

    def getTopics: List[Topic] = {
      ac.describeTopics(listTopics.asJavaCollection).values().asScala.values.map { fd =>
        val desc  = fd.get()
        val parts = desc.partitions().asScala.toList
        Topic(desc.name(), parts.size, parts.head.replicas().size())
      }
    }.toList

    def getTopicConfig(topicName: String): Option[Configuration] =
      getTopics.find(_.name == topicName).fold(Option.empty[Configuration]) { topic =>
        val configEntries = ac
          .describeConfigs(Set(new ConfigResource(ConfigResource.Type.TOPIC, topicName)).asJavaCollection)
          .values()
          .asScala
          .values
          .flatMap(_.get().entries().asScala)
          .toList
        Configuration(topic, configEntries.map(c => ConfigItem(c.name(), c.value(), c.isDefault, c.isReadOnly))).some
      }

  }
}
