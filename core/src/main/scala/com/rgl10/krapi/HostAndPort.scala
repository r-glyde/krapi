package com.rgl10.krapi

import cats.syntax.option._

final case class HostAndPort(host: String, port: Int) {
  val fullname = s"$host:$port"
}

object HostAndPort {
  def apply(fullname: String): Option[HostAndPort] = {
    val validHost = "([a-zA-Z.]+):([0-9]+)".r
    validHost.findFirstMatchIn(fullname).fold(none[HostAndPort]) { m =>
      new HostAndPort(m.group(1), m.group(2).toInt).some
    }
  }
}
