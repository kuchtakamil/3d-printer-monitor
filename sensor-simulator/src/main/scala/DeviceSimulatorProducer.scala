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
import java.time.Instant

object DeviceSimulatorProducer extends IOApp {

    override def run(args: List[String]): IO[ExitCode] = {
        if (args.length != 1 || !List(carriageSpeed, bedTemp).contains(args(0)))
            throw new RuntimeException("invalid argument")

        implicit lazy val simValEncoder: Encoder[SimValue] = deriveEncoder[SimValue]
        val deviceType = args(0)
        val printer = Printer.noSpaces
        val sender = new KafkaSender

        val program1 = for {
            producer <- makeKafkaProducer
            ref <- Resource.eval(Ref[IO].of(1))
            simulator <- Resource.eval(createSimulator)
            generator = new Generator(simulator, deviceType, ref)
            cfg <- Resource.eval(generator.getCfgPayload)
            _ <- Resource.eval(ref.set(cfg.initValue))
        } yield {
            generator.generate
              .map(newVal => createSimValue(deviceType, newVal))
              .map(m => printer.print(m.asJson))
              .flatMap(value => sender.send(producer, deviceType, value))
              .flatMap(_ => IO.sleep(cfg.frequency.second))
        }
        program1.useForever.as((ExitCode.Success))

        val program =
            for {
                ref <- Ref[IO].of(1)
                simulator <- createSimulator
                generator = new Generator(simulator, deviceType, ref)
                cfg <- generator.getCfgPayload
                _ <- ref.set(cfg.initValue)
                _ <- makeKafkaProducer.use((producer: Producer[IO]) => {
                    for {
                        _ <- (
                          generator.generate
                            .map(newVal => createSimValue(deviceType, newVal))
                            .map(m => printer.print(m.asJson))
                            .flatMap(value => sender.send(producer, deviceType, value))
                            .flatMap(_ => IO.sleep(cfg.frequency.second))
                          ).foreverM
                    } yield ()
                })
            } yield ()
        program.as(ExitCode.Success)
    }

    private def createSimulator: IO[Simulator] =
        for {
            simulator <- IO.delay {
                ConfigSource.default.at("simulator").load[Simulator]
            }
            simulator <- simulator.fold(
        err => IO.raiseError(new RuntimeException(s"parsing failed $err")),
        IO.pure)
        } yield simulator

    private def createSimValue(deviceType: String, newVal: Int): SimValue = deviceType match {
        case SimulatorConfig.bedTemp => BedTemperature(Instant.now(), newVal)
        case SimulatorConfig.carriageSpeed => CarriageSpeed(Instant.now(), newVal)
        case _ => CarriageSpeed(Instant.now(), newVal)
    }
}
