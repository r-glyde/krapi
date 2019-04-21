package com.rgl10.krapi

import java.lang.{Long => JLong}
import java.nio.channels.AsynchronousChannelGroup
import java.util.UUID
import java.util.concurrent.Executors

import cats.effect.{ContextShift, IO, Timer}
import cats.syntax.option._
import com.rgl10.krapi.config.KrapiConfig
import io.circe.generic.auto._
import io.circe.syntax._
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.admin.{AdminClient, AdminClientConfig}
import org.apache.kafka.common.serialization.Deserializer
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.{HttpRoutes, Request, Response}
import spinoco.fs2.kafka
import spinoco.fs2.kafka._
import spinoco.protocol.kafka._

import scala.collection.JavaConverters._
import scala.language.higherKinds

class Api(config: KrapiConfig) {

  type RecordStream[K, V] = fs2.Stream[IO, Record[K, V]]

  // TODO - better way to create these?
  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val cs: ContextShift[IO]                      = IO.contextShift(global)
  implicit val timer: Timer[IO]                          = IO.timer(global)
  lazy val executor                                      = Executors.newSingleThreadExecutor()
  implicit val group: AsynchronousChannelGroup           = AsynchronousChannelGroup.withThreadPool(executor)
  implicit val logger                                    = new KafkaLogger()
  implicit val avroValDeser: Deserializer[GenericRecord] = toAvroDeserializer(config.schemaRegistry.value, false)
  implicit val avroKeyDeser: Deserializer[GenericRecord] = toAvroDeserializer(config.schemaRegistry.value, true)

  val kac = KafkaAdminClient(
    AdminClient.create(
      Map[String, AnyRef](AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG -> config.kafkaBrokers.fullname).asJava))

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
            val p = c.topic.partitions
            (kD, vD) match {
              case ("long", "long") =>
                Ok(filteredByKey(key)(consumeFromTopic[JLong, JLong](t, p)(longDeser, longDeser)))
              case ("long", "string") =>
                Ok(filteredByKey(key)(consumeFromTopic[JLong, String](t, p)(longDeser, stringDeser)))
              case ("long", "avro") =>
                Ok(filteredByKey(key)(consumeFromTopic[JLong, GenericRecord](t, p)(longDeser, avroValDeser)))
              case ("string", "long") =>
                Ok(filteredByKey(key)(consumeFromTopic[String, JLong](t, p)(stringDeser, longDeser)))
              case ("string", "avro") =>
                Ok(filteredByKey(key)(consumeFromTopic[String, GenericRecord](t, p)(stringDeser, avroValDeser)))
              case ("avro", "long") =>
                Ok(filteredByKey(key)(consumeFromTopic[GenericRecord, JLong](t, p)(avroKeyDeser, longDeser)))
              case ("avro", "string") =>
                Ok(filteredByKey(key)(consumeFromTopic[GenericRecord, String](t, p)(avroKeyDeser, stringDeser)))
              case ("avro", "avro") =>
                Ok(filteredByKey(key)(consumeFromTopic[GenericRecord, GenericRecord](t, p)(avroKeyDeser, avroValDeser)))
              case _ =>
                Ok(filteredByKey(key)(consumeFromTopic[String, String](t, p)(stringDeser, stringDeser)))
            }
          })
    }
  }

  def filteredByKey[K, V](key: Option[String]): RecordStream[K, V] => RecordStream[K, V] =
    stream => key.fold(stream)(k => stream.filter(_.key.toString == k))

  // TODO - deal with empty topics
  def consumeFromTopic[K, V](topicName: String, partitions: Int)(
      implicit keyDeserializer: Deserializer[K],
      valueDeserializer: Deserializer[V]): RecordStream[K, V] =
    kafka
      .client[IO](Set(broker(config.kafkaBrokers.host, config.kafkaBrokers.port)),
                  ProtocolVersion.Kafka_0_10_2,
                  UUID.randomUUID().toString)
      .flatMap { kc =>
        (0 until partitions).toList.map { p =>
          kc.subscribe(topic(topicName), partition(p), HeadOffset)
            .takeThrough(m => (m.offset + 1) < m.tail)
            .map(_.asRecord[K, V](topicName, p))
        }.reduce(_.merge(_))
      }

  val router: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "metadata" / MetadataType(t) / n                          => toMetadataResponse(t, n.some)
    case GET -> Root / "metadata" / MetadataType(t) :? MaybeEntityNameMatcher(n) => toMetadataResponse(t, n)
    case req @ POST -> Root / "consumer" :? MaybeKeyMatcher(key)                 => toConsumerResponse(req, key)
  }
}

object MaybeEntityNameMatcher extends OptionalQueryParamDecoderMatcher[String]("entityName")
object MaybeKeyMatcher        extends OptionalQueryParamDecoderMatcher[String]("key")
