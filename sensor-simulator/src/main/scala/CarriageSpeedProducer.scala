import cats.effect.{ExitCode, IO, IOApp}
import cats.effect.unsafe.implicits.global
import com.evolutiongaming.skafka.producer.{Producer, ProducerConfig, ProducerRecord, RecordMetadata}

import java.time.Instant

object CarriageSpeedProducer extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    val producerCfg: ProducerConfig = ProducerConfig.Default
    val producer = Producer.of[IO](producerCfg)
    val metadata: IO[RecordMetadata] = producer.use { producer =>
      val record = ProducerRecord(topic = "topic", key = CarriageSpeed(1, Instant.now(), 123), value = "value")
      producer.send(record).flatten
    }
    metadata.as(ExitCode.Success)
  }
}
