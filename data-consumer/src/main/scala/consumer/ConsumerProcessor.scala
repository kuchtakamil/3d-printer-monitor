package consumer

import cats.effect.kernel.Async
import cats.effect.std.Queue
import cats.syntax.all._
import com.evolutiongaming.skafka.consumer._
import fs2.concurrent.Topic
import io.circe.generic.encoding.DerivedAsObjectEncoder.deriveEncoder
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Printer}
import model.config.DataConsumerConfig.{ValidRanges, ValidValueRange}
import model.consumer.ConsumerModel.ClassifiedValue
import model.simulator.SimulatorModel.{BedTemperature, CarriageSpeed, SimValue}

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

trait ConsumerProcessor[F[_]] {
  def consumeMsgFromKafka(queue: Queue[F, SimValue]): F[Unit]
  def poll(): F[List[String]]
  def pushMsg(queue: Queue[F, ClassifiedValue], topic: Topic[F, String], printer: Printer): F[Unit]
  def classify(
    kafkaQueue: Queue[F, SimValue],
    classifiedQueue: Queue[F, ClassifiedValue],
    validRanges: ValidRanges,
  ): F[Unit]
}

object ConsumerProcessor {

  def of[F[_]: Async](
    consumer: Consumer[F, String, String]
  )(implicit simValDecoder: Decoder[SimValue]): ConsumerProcessor[F] = new ConsumerProcessor[F] {

    def consumeMsgFromKafka(queue: Queue[F, SimValue]): F[Unit] = {
      poll()
        .flatMap { msgList =>
          msgList.traverse_ { msg =>
            offerMessageToQueue(queue, msg)
          }
        }
    }

    def poll(): F[List[String]] =
      consumer.poll(1 second).map { records: ConsumerRecords[String, String] =>
        records.values.values
          .flatMap(_.toList)
          .collect { case ConsumerRecord(_, _, _, _, Some(withSize), _) => withSize.value }
          .toList
      }

    def pushMsg(queue: Queue[F, ClassifiedValue], topic: Topic[F, String], printer: Printer): F[Unit] =
      for {
        item <- queue.take
        json  = printer.print(item.asJson)
        _    <- topic.publish1(json)
      } yield ()

    def classify(
      kafkaQueue: Queue[F, SimValue],
      classifiedQueue: Queue[F, ClassifiedValue],
      validRanges: ValidRanges,
    ): F[Unit] =
      for {
        simValue   <- kafkaQueue.take
        classified <- checkIfInRange(simValue, validRanges)
        _          <- classifiedQueue.offer(classified)
      } yield ()

    private def offerMessageToQueue(queue: Queue[F, SimValue], msg: String): F[Unit] = {
      Async[F]
        .fromEither(decode[SimValue](msg))
        .flatMap(simValue => queue.offer(simValue))
        .handleErrorWith { err => Async[F].delay(println(s"Error during message parsing, msg: $msg, err: $err")) }
    }

    private def checkIfInRange(simValue: SimValue, validRanges: ValidRanges): F[ClassifiedValue] = {
      val validateValue =
        (simValue: SimValue, validRange: ValidValueRange, consumerFn: (String, Int, String) => ClassifiedValue) =>
          {
            if (validRange.min < simValue.value && simValue.value < validRange.max)
              Async[F].delay {
                consumerFn(simValue.updatedOn, simValue.value, "valid")
              }
            else
              Async[F].delay {
                consumerFn(simValue.updatedOn, simValue.value, "invalid")
              }
          }: F[ClassifiedValue]

      simValue match {
        case carriageSpeed: CarriageSpeed   =>
          validateValue(
            carriageSpeed,
            validRanges.carriageSpeed.validValueRange,
            model.consumer.ConsumerModel.CarriageSpeed,
          )
        case bedTemperature: BedTemperature =>
          validateValue(
            bedTemperature,
            validRanges.bedTemp.validValueRange,
            model.consumer.ConsumerModel.BedTemperature,
          )
      }
    }
  }
}
