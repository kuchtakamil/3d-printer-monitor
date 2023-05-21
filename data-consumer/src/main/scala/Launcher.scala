import cats.data.NonEmptySet
import cats.effect.std.Queue
import cats.effect.unsafe.implicits.global
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import com.evolutiongaming.skafka.consumer._
import config.ConfigProvider
import consumer.ConsumerProcessor
import fs2.concurrent.Topic
import io.circe.generic.semiauto.deriveDecoder
import io.circe.{Decoder, Printer}
import model.config.SimulatorConfig._
import model.consumer.ConsumerModel.ClassifiedValue
import model.simulator.SimulatorModel.SimValue
import sender.WebSocket

import scala.collection.immutable.SortedSet

object Launcher extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    val argsSet: Set[String] = args(0).split(",").map(_.trim).toSet

    if (args.length != 1 || !argsSet.forall(Set(carriageSpeed, bedTemp).contains))
      throw new RuntimeException(s"consumer invalid argument: $argsSet")

    val deviceTypes: NonEmptySet[String] =
      NonEmptySet.fromSet(SortedSet.empty[String] ++ argsSet).get

    val configProvider  = ConfigProvider.make[IO]
    val webSocketServer = WebSocket.make[IO]
    val consumer        = Consumer.of[IO, String, String](configProvider.customKafkaCfg)

    implicit lazy val simValDecoder: Decoder[SimValue] = deriveDecoder[SimValue]

    val program = consumer.use { consumer =>
      for {
        topic            <- Topic[IO, String]
        consumerProcessor = ConsumerProcessor.of[IO](consumer)
        webSocketCfg     <- configProvider.webSocketCfg
        validRanges      <- configProvider.validRanges
        _                <- consumer.subscribe(deviceTypes, None)
        kafkaQueue       <- Queue.unbounded[IO, SimValue]
        _                <- consumerProcessor.consumeMsgFromKafka(kafkaQueue).foreverM.start
        classifiedQueue  <- Queue.unbounded[IO, ClassifiedValue]
        _                <- consumerProcessor.classify(kafkaQueue, classifiedQueue, validRanges).foreverM.start
        _                <- consumerProcessor.pushMsg(classifiedQueue, topic, Printer.noSpaces).foreverM.start
        _                <- webSocketServer.runWebSocketServer(webSocketCfg, topic).useForever
      } yield ()
    }
    program.as(ExitCode.Success)
  }
}
