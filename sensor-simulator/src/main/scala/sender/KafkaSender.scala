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
  def makeKafkaProducer(configProvider: ConfigProvider[IO]): Resource[IO, Producer[IO]] =
    Producer.of[IO](configProvider.customKafkaCfg)
}

//package sender
//
//import cats.effect.kernel.Async
//import cats.effect.unsafe.implicits.global
//import cats.syntax.all._
//import cats.effect.{IO, Resource}
//import com.evolutiongaming.skafka.producer.{Producer, ProducerRecord, RecordMetadata}
//import config.ConfigProvider
//
//trait KafkaSender[F[_]] {
//  def send(producer: Producer[F], topic: String, payload: String): F[RecordMetadata]
//  def makeKafkaProducer(configProvider: ConfigProvider[F]): Resource[F, Producer[F]]
//}
//
//object KafkaSender {
//  def make[F[_]: Async]: KafkaSender[F] = new KafkaSender[F] {
//
//    def send(producer: Producer[F], topic: String, payload: String): F[RecordMetadata] =
//      producer
//        .send(
//          ProducerRecord(topic = topic, key = s"$topic-01", value = payload)
//        )
//        .flatten
//
//    def makeKafkaProducer(configProvider: ConfigProvider[F]): Resource[F, Producer[F]] =
//      Producer.of[F](configProvider.customKafkaCfg)
//  }
//}
