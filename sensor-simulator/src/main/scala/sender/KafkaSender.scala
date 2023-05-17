package sender

import cats.effect.kernel.Async
import cats.syntax.all._
import com.evolutiongaming.skafka.ToBytes
import com.evolutiongaming.skafka.producer.{Producer, ProducerRecord, RecordMetadata}

trait KafkaSender[F[_]] {
  def send(topic: String, payload: String): F[RecordMetadata]
}

object KafkaSender {
  def make[F[_]: Async](producer: Producer[F])(implicit toBytes: ToBytes[F, String]): KafkaSender[F] =
    (topic: String, payload: String) =>
      producer
        .send(
          ProducerRecord(topic = topic, key = s"$topic-01", value = payload)
        )
        .flatten
}
