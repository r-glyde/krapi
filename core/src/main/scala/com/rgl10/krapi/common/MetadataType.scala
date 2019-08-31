package com.rgl10.krapi.common

import cats.syntax.option._

sealed trait MetadataType extends Product with Serializable

object MetadataType {

  case object Topics         extends MetadataType
  case object ConsumerGroups extends MetadataType

  def unapply(str: String): Option[MetadataType] = str match {
    case "topics"         => Topics.some
    case "consumergroups" => ConsumerGroups.some
    case _                => none[MetadataType]
  }
}
