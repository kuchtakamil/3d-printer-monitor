import ConfigDomain._
import cats.Monad
import cats.effect.{ExitCode, IO, IOApp}
import cats.effect.kernel.Resource
import io.circe.syntax._
import io.circe.{Encoder, Printer}
import io.circe.generic.semiauto.deriveEncoder
import pureconfig._
import pureconfig.generic.auto._

import scala.concurrent.duration._
import java.time.Instant

object DeviceSimulatorProducer extends IOApp {

    implicit val encoder: Encoder[CarriageSpeed] = deriveEncoder[CarriageSpeed]

    override def run(args: List[String]): IO[ExitCode] = {
        println(args.length)
        if (args.length != 1 || !List(carriageSpeed, bedTemp).contains(args(0)))
            println("invalid argument")

        val deviceType = args(0)
        val printer = Printer.noSpaces
        val simulator: IO[Simulator] = createSimulator
        val generator: Generator = new Generator(simulator, deviceType)
        val sender = KafkaSender
        val cfg: IO[ConfigPayload] = generator.getCfgPayload

        val program =
            for {
                randomVal <- generator.generate
                carriageSpeed = CarriageSpeed(Instant.now(), randomVal)
                jsonString = printer.print(carriageSpeed.asJson)
                cfg <- cfg
                _ <- sender.send(deviceType, jsonString)
                _ <- IO.sleep(cfg.frequency.seconds)
            } yield ()
        program.foreverM.as(ExitCode.Success)
    }

    private def createSimulator: IO[Simulator] = {
        for {
            simulator <- IO.delay {
                ConfigSource.default.at("simulator").load[Simulator]
            }
            simulator <- simulator.fold(
                err => IO.raiseError(new RuntimeException(s"parsing failed $err")),
                IO.pure)
        } yield simulator
    }
}
