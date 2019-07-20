package com.rgl10.krapi

import cats.effect.{ContextShift, IO, Timer}
import cats.syntax.option._
import com.rgl10.krapi.config.KrapiConfig
import io.circe.generic.auto._
import io.circe.syntax._
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.common.serialization.{Deserializer, LongDeserializer, StringDeserializer}
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.{HttpRoutes, Request, Response}

class Api(config: KrapiConfig)(implicit cs: ContextShift[IO], timer: Timer[IO]) {

  implicit val avroValDeser: Deserializer[GenericRecord] = toAvroDeserializer(config.schemaRegistry.value, false)
  implicit val avroKeyDeser: Deserializer[GenericRecord] = toAvroDeserializer(config.schemaRegistry.value, true)

  val kac = new KafkaAdminClient(config.kafkaBrokers.map(_.fullname).mkString(","))

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
            val consumer = new StreamingConsumer(config)
            Ok(filteredByKey(key)(consumer.streamTopic(t, c.topic.partitions)))
          })
    }
  }

  def filteredByKey(key: Option[String]): RecordStream => RecordStream =
    stream => key.fold(stream)(k => stream.filter(r => r.key.isDefined && r.key.get == k))

  val router: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "metadata" / MetadataType(t) / n                          => toMetadataResponse(t, n.some)
    case GET -> Root / "metadata" / MetadataType(t) :? MaybeEntityNameMatcher(n) => toMetadataResponse(t, n)
    case req @ POST -> Root / "consumer" :? MaybeKeyMatcher(key)                 => toConsumerResponse(req, key)
  }
}

object MaybeEntityNameMatcher extends OptionalQueryParamDecoderMatcher[String]("entityName")
object MaybeKeyMatcher        extends OptionalQueryParamDecoderMatcher[String]("key")
