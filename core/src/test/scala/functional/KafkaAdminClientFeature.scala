package functional

import base._
import cats.syntax.option._
import cats.data.NonEmptyList
import eu.timepit.refined.api.Refined
import eu.timepit.refined.generic._
import eu.timepit.refined.string._
import net.manub.embeddedkafka.EmbeddedKafka._
import com.rgl10.krapi.KafkaAdminClient
import com.rgl10.krapi._

class KafkaAdminClientFeature extends FeatureSpecBase {

  "KafkaAdminClient" should {
    "list topics with their basic configuration properties" in new TestContext {
      withRunningKafka {
        topics.foreach(t => createCustomTopic(t.name, Map.empty, t.partitions, t.replicationFactor))
        new KafkaAdminClient(defaultKafkaBroker.fullname).getTopics should contain only (topics: _*)
      }
    }

    "return Configuration for corresponding topic" in new TestContext {
      withRunningKafka {
        createCustomTopic(testTopic.name, Map("cleanup.policy" -> "compact"), testTopic.partitions, 1)
        val configuration = new KafkaAdminClient(defaultKafkaBroker.fullname).getTopicConfig(testTopic.name)
        configuration.get.topic shouldBe testTopic
        configuration.get.config.map(c => (c.name -> c.value)) should contain("cleanup.policy" -> "compact")
      }
    }

    "return None when topic for requested config does not exist" in new TestContext {
      withRunningKafka {
        new KafkaAdminClient(defaultKafkaBroker.fullname).getTopicConfig(random[String]) shouldBe none[Configuration]
      }
    }
  }

  private class TestContext {
    val defaultKafkaBroker = HostAndPort("localhost", 6001)
    val topics             = random[NonEmptyList[Topic]].toList
    val testTopic          = topics.head
  }
}
