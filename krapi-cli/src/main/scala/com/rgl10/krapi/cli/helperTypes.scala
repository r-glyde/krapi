package com.rgl10.krapi.cli

sealed trait Deserializers extends Product with Serializable {
  def toValue: String = this match {
    case StringDeserializer => "string"
    case LongDeserializer   => "long"
    case AvroDeserializer   => "avro"
  }
}

case object StringDeserializer extends Deserializers
case object LongDeserializer   extends Deserializers
case object AvroDeserializer   extends Deserializers

sealed trait Mode extends Product with Serializable

case object Consumer extends Mode
case object Metadata extends Mode
