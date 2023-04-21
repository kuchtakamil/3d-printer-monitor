import cats.data.{NonEmptyList, NonEmptySet}
import cats.effect.std.Queue
import cats.effect.unsafe.implicits.global
import cats.effect.{ExitCode, IO, IOApp}
import com.evolutiongaming.skafka.CommonConfig
import com.evolutiongaming.skafka.consumer._
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import io.circe.parser.decode
import model.config.SimulatorConfig._
import model.simulator.SimValue

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import cats.implicits._
import com.comcast.ip4s.IpLiteralSyntax
import fs2.concurrent.Topic
import org.http4s.HttpApp
import org.http4s.ember.server._
import org.http4s.server.websocket.WebSocketBuilder2

object KafkaConsumer extends IOApp {

  implicit lazy val simValDecoder: Decoder[SimValue] = deriveDecoder[SimValue]

  override def run(args: List[String]): IO[ExitCode] = {
    if (args.length != 1 || !List(carriageSpeed, bedTemp).contains(args(0)))
      throw new RuntimeException("invalid argument")

    val deviceType: String = args(0)
    val consumer = Consumer.of[IO, String, String](tmpCfg)

    val program = consumer.use { consumer =>
      for {
        queue <- Queue.unbounded[IO, SimValue]
        _ <- consumer.subscribe(NonEmptySet.of(deviceType), None)
        _ <- consumeMsg(queue, consumer).foreverM.start
        topic <- Topic[IO, String]
        _ <- runWebsocketServer(topic).foreverM.start
        _ <- pushMsg(queue, topic).foreverM.start
        // run server here?
//        _ <- IO(println("Press enter to exit"))
//        _ <- IO(scala.io.StdIn.readLine())
      } yield ()
    }
    program.as(ExitCode.Success)
  }

  private def tmpCfg = {
    val kafkaCommonConfig = CommonConfig.Default.copy(
      bootstrapServers = NonEmptyList.one("localhost:9092"),
      clientId = Some("3d-printer-client-id"))

    val cfg: ConsumerConfig = ConsumerConfig.Default.copy(
      common = kafkaCommonConfig,
      groupId = Some("3d-printer-consumer-group"),
      autoOffsetReset = AutoOffsetReset.Earliest)
    cfg
  }

  def consumeMsg(queue: Queue[IO, SimValue], consumer: Consumer[IO, String, String]): IO[Unit] = {
    for {
      // to be wrapped into function
      msgsIterable <- consumer.poll(1 second).map {
        records: ConsumerRecords[String, String] =>
          records
            .values
            .values
            .flatMap(_.toList)
            .collect { case ConsumerRecord(_, _, _, _, Some(withSize), _) => withSize.value }
      }
      msgs <-
        msgsIterable.toList.map(msg => decode[SimValue](msg))
          .traverse {
            case Right(i) => IO.pure(Some(i))
            case Left(err) => IO.pure(println(s"Error during parsing $err")).as(None)
          }.map(list => list.collect { case Some(i) => i })

      _ <- queue.offer(msgs.last)
      _ <- IO.sleep(1.second)
    } yield ()
  }

  def pushMsg(queue: Queue[IO, SimValue], topic: Topic[IO, String]): IO[Unit] = for {
    num <- queue.take
    _   <- topic.publish ???
  }
  yield ()

  def runWebsocketServer[T](topic: Topic[IO, T]): IO[Unit] =
    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"127.0.0.1")
      .withPort(port"9002")
      .withHttpWebSocketApp(httpApp(topic))
      .build

  private def httpApp(topic: Topic[IO, String])(wsb: WebSocketBuilder2[IO]): HttpApp[IO] = {
    myRoute(wsb) // <+> otherRoute(chatTopic)(wsb)
  }.orNotFound

}