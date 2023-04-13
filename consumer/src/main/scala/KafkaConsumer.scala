import cats.Monad
import cats.data.NonEmptySet
import cats.effect.{ExitCode, IO, IOApp}
import cats.effect.unsafe.implicits.global
import com.evolutiongaming.skafka.consumer.{Consumer, ConsumerConfig, ConsumerRecords}
import scala.concurrent.duration.DurationInt
//
object KafkaConsumer extends IOApp {

  def consumeMsg(consumer: Consumer[IO, String, String]): IO[Unit] =
    IO.delay {
      consumer.poll(100.millis)
      .flatMap(records => IO(records.values.foreach(record => println(s"Consumed message: ${record.toString()}"))))
      .flatMap(_ => consumeMsg(consumer))
    }

  override def run(args: List[String]): IO[ExitCode] = {
    val cfg: ConsumerConfig = ConsumerConfig.Default
    val consumer = Consumer.of[IO, String, String](cfg)

    val program = consumer.use { consumer =>
      for {
        _ <- consumer.subscribe(NonEmptySet.of("bed-temp-01"), None)
        _ <- consumeMsg(consumer).start
        _ <- IO(println("Press enter to exit"))
        _ <- IO(scala.io.StdIn.readLine())
      } yield ()
    }
    program.as(ExitCode.Success)
  }
}