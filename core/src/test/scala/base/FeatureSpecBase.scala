package base

import cats.scalatest.EitherMatchers
import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator
import net.manub.embeddedkafka.EmbeddedKafka
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatest.prop.PropertyChecks

abstract class FeatureSpecBase
    extends WordSpecLike
    with Matchers
    with ScalaFutures
    with RandomDataGenerator
    with EmbeddedKafka
