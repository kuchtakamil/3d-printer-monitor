import cats.data.NonEmptySet
import cats.effect.std.Queue
import cats.effect.unsafe.implicits.global
import cats.effect.{ExitCode, IO, IOApp}
import com.evolutiongaming.skafka.consumer._
import io.circe.{Decoder, Printer}
import io.circe.generic.semiauto.deriveDecoder
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.generic.encoding.DerivedAsObjectEncoder.deriveEncoder
import io.circe.syntax.EncoderOps
import model.config.SimulatorConfig._
import model.simulator.{BedTemperature, CarriageSpeed, SimValue}

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import cats.implicits._
import config.ConfigProvider
import fs2.concurrent.Topic
import model.config.ConsumerConfig.{ValidRanges, ValidValueRange}
import model.consumer.ClassifiedValue
import sender.WebSocket

import scala.collection.immutable.SortedSet

object KafkaConsumer extends IOApp {

  implicit lazy val simValDecoder: Decoder[SimValue] = deriveDecoder[SimValue]

  override def run(args: List[String]): IO[ExitCode] = {
    val argsSet: Set[String] = args(0).split(",").map(_.trim).toSet

    if (args.length != 1 || !argsSet.forall(Set(carriageSpeed, bedTemp).contains))
      throw new RuntimeException(s"consumer invalid argument: $argsSet")

    val deviceTypes: NonEmptySet[String] =
      NonEmptySet.fromSet(SortedSet.empty[String] ++ argsSet).get

    val consumer        = Consumer.of[IO, String, String](ConfigProvider.customKafkaCfg)
    val webSocketServer = WebSocket.of[IO]

    val program = consumer.use { consumer =>
      for {
        topic           <- Topic[IO, String]
        webSocketCfg    <- ConfigProvider.webSocketCfg[IO]
        validRanges     <- ConfigProvider.validRanges[IO]
        _               <- consumer.subscribe(deviceTypes, None)
        kafkaQueue      <- Queue.unbounded[IO, SimValue]
        _               <- consumeMsgFromKafka(kafkaQueue, consumer).foreverM.start
        classifiedQueue <- Queue.unbounded[IO, ClassifiedValue]
        _               <- classify(kafkaQueue, classifiedQueue, validRanges).foreverM.start
        printer          = Printer.noSpaces
        _               <- pushMsg(classifiedQueue, topic, printer).foreverM.start
        _               <- webSocketServer.runWebSocketServer(webSocketCfg, topic).useForever
      } yield ()
    }
    program.as(ExitCode.Success)
  }

  private def consumeMsgFromKafka(queue: Queue[IO, SimValue], consumer: Consumer[IO, String, String]): IO[Unit] = {
    for {
      msgsList      <- poll(consumer)
      msgs          <- msgsList
        .map(msg => decode[SimValue](msg))
        .traverse {
          case Right(i)  => IO.pure(Option(i))
          case Left(err) => IO.delay(println(s"Error during parsing $err")).as(None)
        }
        .map(list => list.collect { case Some(i) => i })
      (csOpt, btOpt) = msgs.foldLeft((none[CarriageSpeed], none[BedTemperature]))(
        (acc: (Option[CarriageSpeed], Option[BedTemperature]), item: SimValue) => {
          item match {
            case bt: BedTemperature => (acc._1, Some(bt))
            case cs: CarriageSpeed  => (Some(cs), acc._2)
          }
        }
      )
      _             <- csOpt match {
        case Some(v) => queue.offer(v)
        case None    => IO.unit
      }
      _             <- btOpt match {
        case Some(v) => queue.offer(v)
        case None    => IO.unit
      }
    } yield ()
  }

  private def poll(consumer: Consumer[IO, String, String]): IO[List[String]] =
    consumer.poll(1 second).map { records: ConsumerRecords[String, String] =>
      records.values.values
        .flatMap(_.toList)
        .collect { case ConsumerRecord(_, _, _, _, Some(withSize), _) => withSize.value }
        .toList
    }

  private def pushMsg(queue: Queue[IO, ClassifiedValue], topic: Topic[IO, String], printer: Printer): IO[Unit] =
    for {
      item <- queue.take
      json  = printer.print(item.asJson)
      _    <- IO(println(json))
      _    <- IO(topic.publish1(json))
    } yield ()

  private def classify(
    kafkaQueue: Queue[IO, SimValue],
    classifiedQueue: Queue[IO, ClassifiedValue],
    validRanges: ValidRanges,
  ): IO[Unit] =
    for {
      simValue   <- kafkaQueue.take
      classified <- checkIfInRange(simValue, validRanges)
      _          <- classifiedQueue.offer(classified)
    } yield ()

  private def checkIfInRange(simValue: SimValue, validRanges: ValidRanges): IO[ClassifiedValue] = {
    val validateValue =
      (simValue: SimValue, validRange: ValidValueRange, consumerFn: (String, Int, String) => ClassifiedValue) =>
        {
          if (validRange.min < simValue.value && simValue.value < validRange.max)
            IO.delay { consumerFn(simValue.updatedOn, simValue.value, "valid") }
          else
            IO.delay { consumerFn(simValue.updatedOn, simValue.value, "invalid") }
        }: IO[ClassifiedValue]

    simValue match {
      case carriageSpeed: CarriageSpeed   =>
        validateValue(carriageSpeed, validRanges.carriageSpeed.validValueRange, model.consumer.CarriageSpeed)
      case bedTemperature: BedTemperature =>
        validateValue(bedTemperature, validRanges.bedTemp.validValueRange, model.consumer.BedTemperature)
    }
  }
}
