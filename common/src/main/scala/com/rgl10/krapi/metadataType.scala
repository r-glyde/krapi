package com.rgl10.krapi

import cats.syntax.option._

sealed trait MetadataType extends Product with Serializable

object MetadataType {
  def unapply(str: String): Option[MetadataType] = str match {
    case "topics" => Topics.some
    case "groups" => ConsumerGroups.some
    case _        => none[MetadataType]
  }
}

case object Topics extends MetadataType
case object ConsumerGroups extends MetadataType