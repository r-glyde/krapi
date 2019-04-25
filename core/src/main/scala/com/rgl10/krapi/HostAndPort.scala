package com.rgl10.krapi

import cats.syntax.option._

final case class HostAndPort(host: String, port: Int) {
  val fullname = s"$host:$port"
}

object HostAndPort {
  def apply(hostName: String): Option[HostAndPort] = {
    val validHost = "([a-zA-Z.]+):([0-9]+)".r
    validHost.findFirstMatchIn(hostName) match {
      case Some(m) => new HostAndPort(m.group(1), m.group(2).toInt).some
      case None    => none[HostAndPort]
    }
  }
}
