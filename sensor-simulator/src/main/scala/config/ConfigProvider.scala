package config

import cats.data.NonEmptyList
import cats.effect.kernel.Async
import cats.syntax.all._
import com.evolutiongaming.skafka.CommonConfig
import com.evolutiongaming.skafka.producer.ProducerConfig
import model.config.SimulatorConfig
import model.config.SimulatorConfig.{ConfigPayload, KafkaConfig, Simulator}
import pureconfig.ConfigReader.Result
import pureconfig.ConfigSource
import pureconfig.generic.auto._

trait ConfigProvider[F[_]] {
  def customKafkaCfg: ProducerConfig
  def simulator: F[Simulator]
  def cfgPayload(simulator: Simulator, deviceType: String): F[ConfigPayload]
}

object ConfigProvider {
  def make[F[_]: Async]: ConfigProvider[F] = new ConfigProvider[F] {

    def customKafkaCfg: ProducerConfig = {
      val kafkaConfig: Result[KafkaConfig] =
        ConfigSource.default.at("simulator-kafka-config").load[KafkaConfig]

      kafkaConfig match {
        case Right(cfg) =>
          val kafkaCommonConfig = CommonConfig.Default.copy(
            bootstrapServers = NonEmptyList.one(s"${cfg.host}:${cfg.port}")
          )
          ProducerConfig.Default.copy(
            common = kafkaCommonConfig
          )
        case Left(_)    => ProducerConfig.Default
      }
    }

    def simulator: F[Simulator] =
      for {
        simulator <- Async[F].delay {
          ConfigSource.default.at("simulator").load[Simulator]
        }
        simulator <- simulator.fold(
          err => Async[F].raiseError(new RuntimeException(s"simulator parsing failed $err")),
          Async[F].pure,
        )
      } yield simulator

    def cfgPayload(simulator: Simulator, deviceType: String): F[ConfigPayload] =
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
}
