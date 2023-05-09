package sender

import cats.data.NonEmptyList
import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
import com.evolutiongaming.skafka.CommonConfig
import com.evolutiongaming.skafka.producer.{Producer, ProducerConfig, ProducerRecord, RecordMetadata}

class KafkaSender {
  def send(producer: Producer[IO], topic: String, payload: String): IO[RecordMetadata] =
    producer
      .send(
        ProducerRecord(topic = topic, key = s"$topic-01", value = payload)
      )
      .flatten
}

object KafkaSender {
  def makeKafkaProducer(): Resource[IO, Producer[IO]] = {
    val kafkaCommonConfig = CommonConfig.Default.copy(
      bootstrapServers = NonEmptyList.one("kafka:9093")
    )

    val cfg = ProducerConfig.Default.copy(
      common = kafkaCommonConfig
    )
    Producer.of[IO](cfg)
  }
}
