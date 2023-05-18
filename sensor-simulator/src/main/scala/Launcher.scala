import cats.effect.unsafe.implicits.global
import cats.effect.{ExitCode, IO, IOApp, Ref, Resource}
import com.evolutiongaming.skafka.producer.Producer
import config.ConfigProvider
import generator.Generator
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import model.config.SimulatorConfig._
import model.simulator.SimulatorModel.SimValue
import producer.DeviceSimulatorProducer
import sender.KafkaSender

object Launcher extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    if (args.length != 1 || !List(carriageSpeed, bedTemp).contains(args(0)))
      throw new RuntimeException(s"producer invalid argument: $args")

    val deviceType     = args(0)
    val configProvider = ConfigProvider.make[IO]

    implicit lazy val simValEncoder: Encoder[SimValue] = deriveEncoder[SimValue]

    val program = for {
      producer               <- Producer.of[IO](configProvider.customKafkaCfg)
      kafkaSender            <- Resource.pure(KafkaSender.make(producer))
      ref                    <- Resource.eval(Ref[IO].of(1))
      simulator              <- Resource.eval(configProvider.simulator)
      generator               = Generator.of[IO](simulator, configProvider, deviceType, ref)
      cfg                    <- Resource.eval(configProvider.cfgPayload(simulator, deviceType))
      _                      <- Resource.eval(ref.set(cfg.initValue))
      deviceSimulatorProducer = DeviceSimulatorProducer.of[IO](kafkaSender, generator, cfg, deviceType)
    } yield {
      deviceSimulatorProducer().foreverM
    }
    program.use(identity).as(ExitCode.Success)
  }
}
