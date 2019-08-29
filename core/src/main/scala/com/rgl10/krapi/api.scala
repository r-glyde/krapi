package com.rgl10.krapi

import cats.effect.{ContextShift, IO, Timer}
import cats.syntax.option._
import com.rgl10.krapi.KafkaAdminClient._
import com.rgl10.krapi.common._
import com.rgl10.krapi.config.KrapiConfig
import fs2.kafka.AdminClientSettings
import io.circe.generic.auto._
import io.circe.syntax._
import org.apache.kafka.common.serialization.{Deserializer, LongDeserializer, StringDeserializer}
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.{HttpRoutes, Request, Response}

class Api(config: KrapiConfig)(implicit cs: ContextShift[IO], timer: Timer[IO]) {

  private val adminClient =
    AdminClientSettings[IO]
      .withBootstrapServers(config.kafkaBrokers.map(_.fullname).mkString(","))
      .createAdminClient

  private val toMetadataResponse: (MetadataType, Option[String]) => IO[Response[IO]] = (mType, mName) => {
    Ok {
      adminClient
        .map(ac =>
          (mType, mName) match {
            case (Topics, None)            => ac.getTopics.asJson
            case (Topics, Some(n))         => ac.getTopicConfig(n).asJson
            case (ConsumerGroups, None)    => ac.getConsumerGroups.asJson
            case (ConsumerGroups, Some(n)) => ac.describeConsumerGroup(n).asJson
        })
    }
  }

  private val toConsumerResponse: (Request[IO], Option[String]) => IO[Response[IO]] = (req, key) => {
    req.as[SubscriptionDetails].flatMap {
      case SubscriptionDetails(t, keyType, valueType) =>
        val keyDeserializer   = deserializerFor(keyType, isKey = true)
        val valueDeserializer = deserializerFor(valueType, isKey = false)
        adminClient.flatMap { ac =>
          ac.getTopicConfig(t)
            .fold(BadRequest())(c => {
              Ok {
                new StreamingConsumer(config)
                  .streamTopic(t, c.topic.partitions)
                  .map(_.toJsonRecord(keyDeserializer, valueDeserializer))
                  .filter(r => key.fold(true)(k => r.key == Option(k.asJson)))
              }
            })
        }
    }
  }

  def deserializerFor(`type`: String, isKey: Boolean): Deserializer[_] = SupportedType.fromString(`type`) match {
    case SupportedType.String => new StringDeserializer
    case SupportedType.Long   => new LongDeserializer
    case SupportedType.Avro   => avroDeserializer(config.schemaRegistry.value, isKey)
  }

  val router: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "metadata" / MetadataType(t) / n                          => toMetadataResponse(t, n.some)
    case GET -> Root / "metadata" / MetadataType(t) :? MaybeEntityNameMatcher(n) => toMetadataResponse(t, n)
    case req @ POST -> Root / "consumer" :? MaybeKeyMatcher(key)                 => toConsumerResponse(req, key)
  }
}

object MaybeEntityNameMatcher extends OptionalQueryParamDecoderMatcher[String]("entityName")
object MaybeKeyMatcher        extends OptionalQueryParamDecoderMatcher[String]("key")
