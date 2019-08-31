package com.rgl10.krapi.common

sealed trait SupportedType extends Product with Serializable {
  def asString: String = this match {
    case SupportedType.String => "string"
    case SupportedType.Long   => "long"
    case SupportedType.Avro   => "avro"
  }
}

object SupportedType {
  def fromString: String => SupportedType = {
    case "string" => String
    case "long"   => Long
    case "avro"   => Avro
  }

  case object String extends SupportedType
  case object Long   extends SupportedType
  case object Avro   extends SupportedType
}
