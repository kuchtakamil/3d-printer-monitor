package sender

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
import com.evolutiongaming.skafka.producer.{Producer, ProducerConfig, ProducerRecord, RecordMetadata}

class KafkaSender {

  private val producerCfg: ProducerConfig = ProducerConfig.Default
  private val producer: Resource[IO, Producer[IO]] = Producer.of[IO](producerCfg)

  def send(topic: String, payload: String): IO[RecordMetadata] = {
    producer.use { producer =>
      val record = ProducerRecord(topic = topic, key = s"$topic-01", value = payload)
      producer.send(record).flatten
    }
  }
}
