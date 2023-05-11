import cats.effect.{Async, ExitCode, IO, IOApp, Ref, Resource}
import com.evolutiongaming.skafka.producer.Producer
import generator.Generator
import io.circe.syntax._
import io.circe.{Encoder, Printer}
import io.circe.generic.semiauto.deriveEncoder
import model.config.SimulatorConfig
import model.config.SimulatorConfig._
import model.simulator.{BedTemperature, CarriageSpeed, SimValue}
import pureconfig._
import pureconfig.generic.auto._
import sender.KafkaSender
import sender.KafkaSender.makeKafkaProducer
import scala.concurrent.duration._

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object DeviceSimulatorProducer extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    if (args.length != 1 || !List(carriageSpeed, bedTemp).contains(args(0)))
      throw new RuntimeException(s"producer invalid argument: $args")

    val deviceType = args(0)
    val program1   = for {
      producer  <- makeKafkaProducer
      ref       <- Resource.eval(Ref[IO].of(1))
      simulator <- Resource.eval(createSimulator)
      generator  = Generator.of[IO](simulator, deviceType, ref)
      cfg       <- Resource.eval(generator.getCfgPayload)
      _         <- Resource.eval(ref.set(cfg.initValue))
    } yield {
      Todo(producer, generator, cfg, deviceType).foreverM
    }
    program1.use(identity).as(ExitCode.Success)
  }

  private def createSimulator: IO[Simulator] =
    for {
      simulator <- IO.delay {
        ConfigSource.default.at("simulator").load[Simulator]
      }
      simulator <- simulator.fold(err => IO.raiseError(new RuntimeException(s"simulator parsing failed $err")), IO.pure)
    } yield simulator

  private def createSimValue(deviceType: String, newVal: Int): SimValue = deviceType match {
    case SimulatorConfig.bedTemp       => BedTemperature(now(), newVal)
    case SimulatorConfig.carriageSpeed => CarriageSpeed(now(), newVal)
    case _                             => CarriageSpeed(now(), newVal)
  }

  private def now(): String =
    LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))

  private object Todo {
    private implicit lazy val simValEncoder: Encoder[SimValue] = deriveEncoder[SimValue]

    private val printer = Printer.noSpaces
    private val sender  = new KafkaSender

    def apply(producer: Producer[IO], generator: Generator[IO], cfg: ConfigPayload, deviceType: String) = {
      generator.generate
        .map(newVal => createSimValue(deviceType, newVal))
        .map(m => printer.print(m.asJson))
        .flatMap(value => sender.send(producer, deviceType, value))
        .flatMap(_ => IO.sleep(cfg.frequency.second))
    }
  }
}
