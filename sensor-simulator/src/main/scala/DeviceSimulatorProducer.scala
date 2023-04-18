import ConfigDomain._
import cats.Monad
import cats.effect.{Async, ExitCode, IO, IOApp, Ref}
import cats.effect.kernel.Resource
import io.circe.syntax._
import io.circe.{Decoder, Encoder, HCursor, Json, Printer}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import pureconfig._
import pureconfig.generic.auto._

import scala.concurrent.duration._
import java.time.Instant

object DeviceSimulatorProducer extends IOApp {


    override def run(args: List[String]): IO[ExitCode] = {
        if (args.length != 1 || !List(carriageSpeed, bedTemp).contains(args(0)))
            println("invalid argument")

        implicit lazy val simValEncoder: Encoder[SimValue] = deriveEncoder[SimValue]
        val deviceType = args(0)
        val printer = Printer.noSpaces
        val sender = new KafkaSender

        val program =
            for {
                ref <- Ref[IO].of(1)
                simulator <- createSimulator
                generator = new Generator(simulator, deviceType, ref)
                cfg <- generator.getCfgPayload
                _ <- ref.set(cfg.initValue)
                _ <- (
                  generator.generate
                    .map(newVal => createObj(deviceType, newVal))
                    .map(m => printer.print(m.asJson))
                    .flatMap(value => sender.send(deviceType, value))
                    .flatMap(_ => IO.sleep(cfg.frequency.second))
                  ).foreverM
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

    private def createObj(deviceType: String, newVal: Int): SimValue = deviceType match {
        case ConfigDomain.bedTemp => BedTemperature(Instant.now(), newVal)
        case ConfigDomain.carriageSpeed => CarriageSpeed(Instant.now(), newVal)
        case _ => CarriageSpeed(Instant.now(), newVal)
    }
}
