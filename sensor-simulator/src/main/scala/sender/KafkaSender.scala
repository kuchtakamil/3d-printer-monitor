package sender

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
import com.evolutiongaming.skafka.producer.{Producer, ProducerRecord, RecordMetadata}
import config.ConfigProvider

class KafkaSender {
  def send(producer: Producer[IO], topic: String, payload: String): IO[RecordMetadata] =
    producer
      .send(
        ProducerRecord(topic = topic, key = s"$topic-01", value = payload)
      )
      .flatten
}

object KafkaSender {
  def makeKafkaProducer(): Resource[IO, Producer[IO]] =
    Producer.of[IO](ConfigProvider.customKafkaCfg)
}
