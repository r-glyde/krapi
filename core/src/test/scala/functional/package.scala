import com.rgl10.krapi.HostAndPort
import com.rgl10.krapi.config.KrapiConfig
import eu.timepit.refined.auto._
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}

package object functional {
  implicit val stringSerializer   = new StringSerializer

  val testConfig = KrapiConfig(List(HostAndPort("localhost", 6001)), "http://localhost:8082")

  implicit class ByteArrayOps(val bytes: Array[Byte]) {
    def deserialize: String = (new StringDeserializer).deserialize("", bytes)
  }
}
