package com.rgl10.krapi


final case class HostAndPort(host: String, port: Int) {
  val fullname = s"$host:$port"
}

object HostAndPort {
  def apply(hostName: String): HostAndPort = {
    val validHost = "([a-zA-Z.]+):([0-9]+)".r
    validHost.findFirstMatchIn(hostName) match {
      case Some(m) => new HostAndPort(m.group(1), m.group(2).toInt)
      case None => throw new Exception("Hostname input cannot be converted to a valid host and port")
    }
  }
}
