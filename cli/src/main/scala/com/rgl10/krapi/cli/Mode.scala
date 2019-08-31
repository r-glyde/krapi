package com.rgl10.krapi.cli

sealed trait Mode extends Product with Serializable

object Mode {
  case object Consumer extends Mode
  case object Metadata extends Mode
}

