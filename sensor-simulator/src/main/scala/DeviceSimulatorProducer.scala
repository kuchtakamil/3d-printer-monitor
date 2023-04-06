import ConfigDomain.{ConfigPayload, Simulator}
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

    def startSending(deviceType: String) {
        val printer = Printer.noSpaces
        val simulator: IO[Simulator] = createSimulator
        val generator: Generator = new Generator(simulator)
        val sender = KafkaSender
        val cfg: IO[ConfigPayload] = generator.getCfgPayload(deviceType)

        val program =
            for {
                randomVal       <- Resource.eval(generator.generate(deviceType))
                carriageSpeed   = CarriageSpeed(Instant.now(), randomVal)
                jsonString      = printer.print(carriageSpeed.asJson)
                cfg             <- Resource.eval(cfg)
                _               <- Resource.eval(sender.send(deviceType, jsonString).flatten)
                _               <- Resource.eval(IO.sleep(cfg.frequency.seconds))
            } yield ()

        program.useForever.as(ExitCode.Success)
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
