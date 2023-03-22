import org.apache.kafka.common.serialization.Serializer

import java.util.Properties
import com.goyeau.kafka.streams.circe.CirceSerdes._
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord, RecordMetadata}
import org.apache.kafka.streams.scala.serialization.Serdes
import org.apache.kafka.streams.scala.serialization.Serdes._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait Record[K, V] {
  def topic: String
  def key(value: V): K
  def timestamp(value: V): Long
}

object Producer {
  def apply[V] = new ProducerBuilder[V]
  class ProducerBuilder[V] {
    def apply[K](config: Properties)(implicit record: Record[K, V],
                                     keySerializer: Serializer[K],
                                     valueSerializer: Serializer[V]): KafkaProducer[K, V] =
      new KafkaProducer(config, keySerializer, valueSerializer)
  }

  implicit class KafkaProducerOps[K, V](kafkaProducer: KafkaProducer[K, V]) {
    def send(value: V)(implicit record: Record[K, V]): Future[RecordMetadata] = Future {
      kafkaProducer.send(new ProducerRecord(record.topic, null, record.timestamp(value), record.key(value), value)).get()
    }
  }
}
object ModelOps {
  object CarriageSpeed {
    implicit val record: Record[Id[CarriageSpeed], CarriageSpeed] = new Record[Id[CarriageSpeed], CarriageSpeed] {
      val topic = "carriage-speed"
      def key(speed: CarriageSpeed): Id[CarriageSpeed] = speed.id
      def timestamp(speed: CarriageSpeed): Long = speed.updatedOn.toEpochMilli
    }
  }

  object BedTemperature {
    implicit val record: Record[Id[BedTemperature], BedTemperature] = new Record[Id[BedTemperature], BedTemperature] {
      val topic = "bed-temperature"
      def key(temp: BedTemperature): Id[BedTemperature] = temp.id
      def timestamp(temp: BedTemperature): Long = temp.updatedOn.toEpochMilli
    }
  }
}