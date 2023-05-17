import cats.effect.unsafe.implicits.global
import cats.effect.{ExitCode, IO, IOApp, Ref, Resource}
import cats.syntax.all._
import com.evolutiongaming.skafka.producer.Producer
import config.ConfigProvider
import generator.Generator
import io.circe.generic.semiauto.deriveEncoder
import io.circe.syntax._
import io.circe.{Encoder, Printer}
import model.config.SimulatorConfig
import model.config.SimulatorConfig._
import model.simulator.SimulatorModel.{BedTemperature, CarriageSpeed, SimValue}
import sender.KafkaSender

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.concurrent.duration._

object DeviceSimulatorProducer extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    if (args.length != 1 || !List(carriageSpeed, bedTemp).contains(args(0)))
      throw new RuntimeException(s"producer invalid argument: $args")

    val deviceType     = args(0)
    val configProvider = ConfigProvider.make[IO]

    val program = for {
      producer    <- Producer.of[IO](configProvider.customKafkaCfg)
      kafkaSender <- Resource.pure(KafkaSender.make(producer))
      ref         <- Resource.eval(Ref[IO].of(1))
      simulator   <- Resource.eval(configProvider.simulator)
      generator    = Generator.of[IO](simulator, configProvider, deviceType, ref)
      cfg         <- Resource.eval(configProvider.cfgPayload(simulator, deviceType))
      _           <- Resource.eval(ref.set(cfg.initValue))
    } yield {
      Program(kafkaSender, generator, cfg, deviceType).foreverM
    }
    program.use(identity).as(ExitCode.Success)
  }

  private def simValue(deviceType: String, newVal: Int): SimValue = deviceType match {
    case SimulatorConfig.bedTemp       => BedTemperature(now(), newVal)
    case SimulatorConfig.carriageSpeed => CarriageSpeed(now(), newVal)
    case _                             => CarriageSpeed(now(), newVal)
  }

  private def now(): String =
    LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))

  private object Program {
    private implicit lazy val simValEncoder: Encoder[SimValue] = deriveEncoder[SimValue]

    private val printer = Printer.noSpaces

    def apply(kafkaSender: KafkaSender[IO], generator: Generator[IO], cfg: ConfigPayload, deviceType: String) = {
      generator.generate
        .map(newVal => simValue(deviceType, newVal))
        .map(m => printer.print(m.asJson))
        .flatMap(value => kafkaSender.send(deviceType, value))
        .flatMap(_ => IO.sleep(cfg.frequency.second))
    }
  }
}
