package com.rgl10.krapi

import cats.effect.IO
import com.rgl10.krapi.common._
import io.circe.generic.auto._
import org.http4s.circe.jsonEncoderOf

package object cli {

  type CliApp = Either[ApplicationError, Unit]

  implicit def subscriptionEncoder = jsonEncoderOf[IO, SubscriptionDetails]

}
