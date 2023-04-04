import ConfigDomain.Simulator
import cats.effect.IO
import io.circe.syntax._
import io.circe.{Encoder, Printer}
import io.circe.generic.semiauto.deriveEncoder
import pureconfig._
import pureconfig.generic.auto._

import java.time.Instant

object DeviceSimulatorProducer {

    implicit val encoder: Encoder[CarriageSpeed] = deriveEncoder[CarriageSpeed]

    def startSending(deviceType: String) {
        val printer = Printer.noSpaces
        val simulator: IO[Simulator] = createSimulator
        val generator: Generator = new Generator(simulator)
        val sender = KafkaSender

        for {
            randomVal <- generator.generate(deviceType)
            carriageSpeed = CarriageSpeed(1, Instant.now(), randomVal)
            jsonString = printer.print(carriageSpeed.asJson)
            _ <- sender.send(deviceType, jsonString)
        } yield ()
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
