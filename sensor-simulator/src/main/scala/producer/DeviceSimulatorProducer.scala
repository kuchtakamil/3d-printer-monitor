package producer

import cats.effect.kernel.Async
import cats.syntax.all._
import generator.Generator
import io.circe.syntax._
import io.circe.{Encoder, Printer}
import model.config.SimulatorConfig
import model.config.SimulatorConfig._
import model.simulator.SimulatorModel.{BedTemperature, CarriageSpeed, SimValue}
import sender.KafkaSender

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.concurrent.duration._

trait DeviceSimulatorProducer[F[_]] {
  def apply(): F[Unit]
}

object DeviceSimulatorProducer {

  def of[F[_]: Async](
    kafkaSender: KafkaSender[F],
    generator: Generator[F],
    cfg: ConfigPayload,
    deviceType: String,
  )(implicit simValEncoder: Encoder[SimValue]): DeviceSimulatorProducer[F] = new DeviceSimulatorProducer[F] {

    def apply(): F[Unit] = {
      generator.generate
        .map(newVal => simValue(deviceType, newVal))
        .map(m => Printer.noSpaces.print(m.asJson))
        .flatMap(value => kafkaSender.send(deviceType, value))
        .flatMap(_ => Async[F].sleep(cfg.frequency.second))
    }

    private def simValue(deviceType: String, newVal: Int): SimValue = deviceType match {
      case SimulatorConfig.bedTemp       => BedTemperature(now(), newVal)
      case SimulatorConfig.carriageSpeed => CarriageSpeed(now(), newVal)
      case _                             => CarriageSpeed(now(), newVal)
    }

    private def now(): String =
      LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
  }
}
