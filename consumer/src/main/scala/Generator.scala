//import cats.Monad
//import cats.effect.{ExitCode, IO, IOApp}
//import cats.effect.kernel.Resource
//import io.circe.syntax._
//import io.circe.{Encoder, Printer}
//import io.circe.generic.semiauto.deriveEncoder
//import java.time.Instant
//
//object DeviceSimulatorProducer extends IOApp {
//
//  implicit val encoder: Encoder[CarriageSpeed] = deriveEncoder[CarriageSpeed]
//
//  override def run(args: List[String]): IO[ExitCode] = {
//
//    val program =
//      for {
//        randomVal <- generator.generate
//      } yield ()
//    program.foreverM.as(ExitCode.Success)
//  }
//}