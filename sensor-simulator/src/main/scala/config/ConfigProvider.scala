package config

import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.kernel.Async
import com.evolutiongaming.skafka.CommonConfig
import com.evolutiongaming.skafka.producer.ProducerConfig
import model.config.ConsumerConfig.KafkaConfig
import model.config.SimulatorConfig
import model.config.SimulatorConfig.{ConfigPayload, Simulator}
import pureconfig.ConfigReader.Result
import pureconfig.ConfigSource
import pureconfig.generic.auto._

object ConfigProvider {

  def customKafkaCfg: ProducerConfig = {
    val kafkaConfig: Result[KafkaConfig] = ConfigSource.default.at("kafka-config").load[KafkaConfig]

    kafkaConfig match {
      case Right(cfg) =>
        val kafkaCommonConfig = CommonConfig.Default.copy(
          bootstrapServers = NonEmptyList.one(s"${cfg.host}:${cfg.port}"),
          clientId = Some(cfg.clientId),
        )
        ProducerConfig.Default.copy(
          common = kafkaCommonConfig
        )
      case Left(_)    => ProducerConfig.Default
    }
  }

  def simulator[F[_]: Async]: F[Simulator] =
    for {
      simulator <- Async[F].delay {
        ConfigSource.default.at("simulator").load[Simulator]
      }
      simulator <- simulator.fold(
        err => IO.raiseError(new RuntimeException(s"simulator parsing failed $err")),
        Async[F].pure(),
      )
    } yield simulator

  def cfgPayload[F[_]: Async](simulator: Simulator, deviceType: String): F[ConfigPayload] =
    chooseDevice(deviceType, simulator) match {
      case Right(v) => Async[F].pure(v)
      case Left(e)  => Async[F].raiseError(new RuntimeException(e))
    }

  private def chooseDevice(deviceType: String, simulator: Simulator): Either[String, ConfigPayload] =
    deviceType match {
      case SimulatorConfig.carriageSpeed => Right(simulator.carriageSpeed)
      case SimulatorConfig.bedTemp       => Right(simulator.bedTemp)
      case _                             => Left("unknown device type")
    }
}
