package functional

import java.nio.channels.AsynchronousChannelGroup
import java.util.concurrent.Executors
import java.util.UUID

import base._
import cats.effect.{ContextShift, IO, Timer}
import cats.syntax.option._
import cats.data.NonEmptyList
import eu.timepit.refined.api.Refined
import eu.timepit.refined.generic._
import eu.timepit.refined.string._
import net.manub.embeddedkafka.EmbeddedKafka._
import org.scalacheck.{Arbitrary, Gen}
import com.rgl10.krapi.KafkaAdminClient
import com.rgl10.krapi._

import scala.concurrent.ExecutionContext

class StreamingConsumerFeature extends FeatureSpecBase {

  "StreamingConsumer" should {
    "return deserialized records from topic of interest" in new TestContext {
      withRunningKafka {
        println(testTopic)
        println(messages)
        publishToKafka(testTopic.name, messages.take(1))

        val consumer = new StreamingConsumer(defaultKafkaBroker)

        val x = consumer.streamTopic(testTopic.name, testTopic.partitions).compile.toList.unsafeRunSync

        println(x)
      }
    }
  }

  private class TestContext {
    implicit val arbNonEmptyAlphaStr: Arbitrary[String] = Arbitrary(Gen.nonEmptyListOf(Gen.alphaChar).map(_.mkString))

    lazy val executor  = Executors.newSingleThreadExecutor()
    implicit val group = AsynchronousChannelGroup.withThreadPool(executor)
    implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
    implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)
    implicit val logger: KafkaLogger = new KafkaLogger()

    val defaultKafkaBroker = HostAndPort("localhost", 6001)
    val testTopic          = random[Topic]
    val messages           = random[NonEmptyList[String]].map((UUID.randomUUID().toString -> _)).toList
  }
}
