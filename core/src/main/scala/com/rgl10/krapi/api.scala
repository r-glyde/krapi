package com.rgl10.krapi

import java.lang.{Long => JLong}
import java.nio.channels.AsynchronousChannelGroup
import java.util.UUID

import cats.effect.{ContextShift, IO, Timer}
import cats.syntax.option._
import com.rgl10.krapi.config.KrapiConfig
import io.circe.generic.auto._
import io.circe.syntax._
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.admin.{AdminClient, AdminClientConfig}
import org.apache.kafka.common.serialization.{Deserializer, LongDeserializer, StringDeserializer}
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.{HttpRoutes, Request, Response}
import spinoco.fs2.kafka
import spinoco.fs2.kafka._
import spinoco.protocol.kafka._

import scala.collection.JavaConverters._

class Api(config: KrapiConfig)(implicit cs: ContextShift[IO], timer: Timer[IO], group: AsynchronousChannelGroup) {

  implicit val logger: KafkaLogger                       = new KafkaLogger()
  implicit val stringDeser: StringDeserializer           = new StringDeserializer()
  implicit val longDeser: LongDeserializer               = new LongDeserializer()
  implicit val avroValDeser: Deserializer[GenericRecord] = toAvroDeserializer(config.schemaRegistry.value, false)
  implicit val avroKeyDeser: Deserializer[GenericRecord] = toAvroDeserializer(config.schemaRegistry.value, true)

  val kac = new KafkaAdminClient(config.kafkaBrokers.fullname)

  private val toMetadataResponse: (MetadataType, Option[String]) => IO[Response[IO]] = {
    case (Topics, None)            => Ok(kac.getTopics.asJson)
    case (Topics, Some(n))         => Ok(kac.getTopicConfig(n).asJson)
    case (ConsumerGroups, None)    => NotImplemented()
    case (ConsumerGroups, Some(_)) => NotImplemented()
  }

  // TODO - better way to do this?
  private val toConsumerResponse: (Request[IO], Option[String]) => IO[Response[IO]] = (req, key) => {
    req.as[SubscriptionDetails].flatMap {
      case SubscriptionDetails(t, kD, vD) =>
        kac
          .getTopicConfig(t)
          .fold(BadRequest())(c => {
            val consumer = new StreamingConsumer(config.kafkaBrokers)
            val p = c.topic.partitions
            (kD, vD) match {
              case ("long", "long") =>
                Ok(filteredByKey(key)(consumer.streamTopic[JLong, JLong](t, p)(longDeser, longDeser)))
              case ("long", "string") =>
                Ok(filteredByKey(key)(consumer.streamTopic[JLong, String](t, p)(longDeser, stringDeser)))
              case ("long", "avro") =>
                Ok(filteredByKey(key)(consumer.streamTopic[JLong, GenericRecord](t, p)(longDeser, avroValDeser)))
              case ("string", "long") =>
                Ok(filteredByKey(key)(consumer.streamTopic[String, JLong](t, p)(stringDeser, longDeser)))
              case ("string", "avro") =>
                Ok(filteredByKey(key)(consumer.streamTopic[String, GenericRecord](t, p)(stringDeser, avroValDeser)))
              case ("avro", "long") =>
                Ok(filteredByKey(key)(consumer.streamTopic[GenericRecord, JLong](t, p)(avroKeyDeser, longDeser)))
              case ("avro", "string") =>
                Ok(filteredByKey(key)(consumer.streamTopic[GenericRecord, String](t, p)(avroKeyDeser, stringDeser)))
              case ("avro", "avro") =>
                Ok(filteredByKey(key)(consumer.streamTopic[GenericRecord, GenericRecord](t, p)(avroKeyDeser, avroValDeser)))
              case _ =>
                Ok(filteredByKey(key)(consumer.streamTopic[String, String](t, p)(stringDeser, stringDeser)))
            }
          })
    }
  }

  def filteredByKey[K, V](key: Option[String]): RecordStream[K, V] => RecordStream[K, V] =
    stream => key.fold(stream)(k => stream.filter(_.key.toString == k))

  val router: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "metadata" / MetadataType(t) / n                          => toMetadataResponse(t, n.some)
    case GET -> Root / "metadata" / MetadataType(t) :? MaybeEntityNameMatcher(n) => toMetadataResponse(t, n)
    case req @ POST -> Root / "consumer" :? MaybeKeyMatcher(key)                 => toConsumerResponse(req, key)
  }
}

object MaybeEntityNameMatcher extends OptionalQueryParamDecoderMatcher[String]("entityName")
object MaybeKeyMatcher        extends OptionalQueryParamDecoderMatcher[String]("key")
