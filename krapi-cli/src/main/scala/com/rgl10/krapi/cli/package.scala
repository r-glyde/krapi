package com.rgl10.krapi

import cats.effect.IO
import io.circe.generic.auto._
import org.http4s.circe.jsonEncoderOf

package object cli {

  implicit def subscriptionEncoder = jsonEncoderOf[IO, SubscriptionDetails]

  implicit val modesRead: scopt.Read[Mode] =
    scopt.Read.reads {
      case "M" => Metadata
      case "C" => Consumer
    }

  implicit val metadataTypesRead: scopt.Read[MetadataType] =
    scopt.Read.reads {
      case "topics" => Topics
      case "groups" => ConsumerGroups
    }

  implicit val deserializersRead: scopt.Read[Deserializers] =
    scopt.Read.reads {
      case "string" => StringDeserializer
      case "long"   => LongDeserializer
      case "avro"   => AvroDeserializer
    }
}
