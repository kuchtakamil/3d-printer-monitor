package sender

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
import com.evolutiongaming.skafka.producer.{Producer, ProducerConfig, ProducerRecord, RecordMetadata}

class KafkaSender {
  def send(producer: Producer[IO], topic: String, payload: String): IO[RecordMetadata] = {
    val record = ProducerRecord(topic = topic, key = s"$topic-01", value = payload)
    producer.send(record).flatten
  }
}

object KafkaSender {
  def makeKafkaProducer(): Resource[IO, Producer[IO]] =
    Producer.of[IO](ProducerConfig.Default)
}
