package functional

import cats.syntax.option._
import cats.data.NonEmptyList
import cats.scalatest.EitherMatchers
import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator
import eu.timepit.refined.api.Refined
import eu.timepit.refined.generic._
import eu.timepit.refined.string._
import net.manub.embeddedkafka.EmbeddedKafka._
import net.manub.embeddedkafka.EmbeddedKafka
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatest.prop.PropertyChecks
import com.rgl10.krapi.KafkaAdminClient
import com.rgl10.krapi._

class KafkaAdminClientFeature
    extends WordSpecLike
    with Matchers
    with ScalaFutures
    with RandomDataGenerator
    with EmbeddedKafka {

  implicit val arbNonEmptyAlphaStr: Arbitrary[String] = Arbitrary(Gen.nonEmptyListOf(Gen.alphaChar).map(_.mkString))

  "KafkaAdminClient" should {

    "list topics with their basic configuration properties" in new TestContext {
      withRunningKafka {
        topicNames.foreach(t => createCustomTopic(t, Map.empty, 1, 1))
        new KafkaAdminClient(defaultKafkaBroker).getTopics.map(_.name) should contain only (topicNames: _*)
      }
    }

    "return Configuration for corresponding topic" in new TestContext {
      withRunningKafka {
        createCustomTopic(testTopic, Map("cleanup.policy" -> "compact"), 1, 1)
        val configuration = new KafkaAdminClient(defaultKafkaBroker).getTopicConfig(testTopic)
        configuration.get.topic.name shouldBe testTopic
        configuration.get.config.map(c => (c.name -> c.value)) should contain ("cleanup.policy" -> "compact")
      }
    }

    "return None when topic for requested config does not exist" in new TestContext {
      withRunningKafka {
        new KafkaAdminClient(defaultKafkaBroker).getTopicConfig(random[String]) shouldBe none[Configuration]
      }
    }
  }

  private class TestContext {
    val defaultKafkaBroker = "localhost:6001"
    val topicNames         = random[NonEmptyList[String]].toList
    val testTopic          = "test-topic-1"
  }
}
