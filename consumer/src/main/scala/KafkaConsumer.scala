import cats.Monad
import cats.data.{NonEmptyList, NonEmptySet}
import cats.effect.{ExitCode, IO, IOApp}
import cats.effect.unsafe.implicits.global
import com.evolutiongaming.skafka.CommonConfig
import com.evolutiongaming.skafka.consumer.{AutoOffsetReset, Consumer, ConsumerConfig, ConsumerRecords}
import io.circe.Decoder
import io.circe.parser.decode
import io.circe.generic.semiauto.deriveDecoder

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object KafkaConsumer extends IOApp {

  implicit lazy val simValDecoder: Decoder[SimValue] = deriveDecoder[SimValue]

  def consumeMsg(consumer: Consumer[IO, String, String]): IO[Unit] =
      consumer.poll(1000.millis)
      .flatMap(records => IO(records.values.foreach(record => println(s"\n\nConsumed message: ${record._2.flatMap(r => r.value.map(ws => ws.value))}"))))
//      .flatMap(records => IO(records.values.foreach(record => println(decode[SimValue](record.toString())))))
      .flatMap(_ => IO.sleep(2 seconds))
      .flatMap(_ => consumeMsg(consumer))

  override def run(args: List[String]): IO[ExitCode] = {
//    val cfg: ConsumerConfig = ConsumerConfig.Default
    val kafkaCommonConfig = CommonConfig.Default.copy(
      bootstrapServers = NonEmptyList.one("localhost:9092"),
      clientId = Some("3d-printer-client-id"))

    val cfg: ConsumerConfig = ConsumerConfig.Default.copy(
      common = kafkaCommonConfig,
      groupId = Some("3d-printer-consumer-group"),
      autoOffsetReset = AutoOffsetReset.Earliest)

    val consumer = Consumer.of[IO, String, String](cfg)

    val program = consumer.use { consumer =>
      for {
        _ <- consumer.subscribe(NonEmptySet.of("bed-temp"), None)
        _ <- IO.sleep(1 second)
        _ <- consumeMsg(consumer).start
        _ <- IO(println("Press enter to exit"))
        _ <- IO(scala.io.StdIn.readLine())
      } yield ()
    }
    program.as(ExitCode.Success)
  }
}