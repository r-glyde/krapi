import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Gen.posNum
import com.rgl10.krapi.Topic

package object functional {

  implicit val stringSer: StringSerializer = new StringSerializer()
  implicit val stringDeser: StringDeserializer = new StringDeserializer()

  implicit val topicGenerator: Arbitrary[Topic] = Arbitrary {
    for {
      topicName  <- Gen.nonEmptyListOf(Gen.alphaChar).map(_.mkString)
      partitions <- Gen.posNum[Int]
    } yield Topic(topicName, partitions, 1)
  }
}
