package sender

import cats.effect.kernel.Async
import cats.effect.unsafe.implicits.global
import cats.syntax.all._
import com.evolutiongaming.catshelper.ToTry
import com.evolutiongaming.skafka.ToBytes
import com.evolutiongaming.skafka.producer.{Producer, ProducerRecord, RecordMetadata}
import config.ConfigProvider
import com.evolutiongaming.skafka.ToBytes._

trait KafkaSender[F[_]] {
  def send(topic: String, payload: String): F[RecordMetadata]
}

object KafkaSender {
  def make[F[_]: Async: ToTry](producer: Producer[F])(implicit toBytes: ToBytes[F, String]): KafkaSender[F] =
    new KafkaSender[F] {

      def send(topic: String, payload: String): F[RecordMetadata] =
        producer
          .send(
            ProducerRecord(topic = topic, key = s"$topic-01", value = payload)
          )
          .flatten
    }
}
