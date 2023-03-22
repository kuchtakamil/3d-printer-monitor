import com.goyeau.kafka.streams.circe.CirceSerdes.serializer
import org.apache.kafka.clients.producer.ProducerConfig

import java.util.Properties

class CarriageSpeedProducer {
  val config = new Properties()
  config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092")
  import ModelOps.CarriageSpeed
  val postProducer = Producer[CarriageSpeed](config)

//  postProducer.send(speed)

  postProducer.close()
}
