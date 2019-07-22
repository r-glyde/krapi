package functional

import java.util.UUID

import base._
import cats.data.NonEmptyList
import cats.effect.{ContextShift, IO, Timer}
import com.rgl10.krapi._
import com.rgl10.krapi.common.Topic
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.refineV
import eu.timepit.refined.types.string.NonEmptyString
import org.scalacheck.{Arbitrary, Gen}

import scala.concurrent.ExecutionContext

class StreamingConsumerFeature extends FeatureSpecBase {

  "StreamingConsumer" should {
    "return records from topic of interest" in new TestContext {
      withRunningKafka {
        publishToKafka(testTopic.name, messages)

        val results = new StreamingConsumer(testConfig)
          .streamTopic(testTopic.name, testTopic.partitions)
          .compile
          .toList
          .unsafeRunSync

        results.length shouldBe messages.length
        results.map(cr => cr.key.deserialize -> cr.value.deserialize) shouldBe messages
      }
    }
  }

  private class TestContext {
    implicit val arbNonEmptyAlphaStr: Arbitrary[NonEmptyString] =
      Arbitrary(Gen.nonEmptyListOf(Gen.alphaChar).map(l => refineV[NonEmpty].unsafeFrom(l.mkString)))

    implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
    implicit val timer: Timer[IO]               = IO.timer(ExecutionContext.global)

    // TODO: streaming consumer does not deal with empty partitions properly
    val testTopic = Topic(random[NonEmptyString].value, 1, 1)
    val messages  = random[NonEmptyList[NonEmptyString]].map(UUID.randomUUID().toString -> _.value).toList
  }
}
