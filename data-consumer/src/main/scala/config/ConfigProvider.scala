package config

import cats.data.NonEmptyList
import cats.syntax.all._
import cats.effect.kernel.Async
import com.evolutiongaming.skafka.CommonConfig
import com.evolutiongaming.skafka.consumer.{AutoOffsetReset, ConsumerConfig}
import model.config.DataConsumerConfig.{KafkaConfig, ValidRanges, WebSocketConfig}
import pureconfig.ConfigReader.Result
import pureconfig.ConfigSource
import pureconfig.generic.auto._

trait ConfigProvider[F[_]] {
  def validRanges: F[ValidRanges]
  def webSocketCfg: F[WebSocketConfig]
  def customKafkaCfg: ConsumerConfig
}

object ConfigProvider {
  def make[F[_]: Async]: ConfigProvider[F] = new ConfigProvider[F] {

    def validRanges: F[ValidRanges] =
      for {
        validRanges <- Async[F].delay {
          ConfigSource.default.at("valid-ranges").load[ValidRanges]
        }
        validRanges <- validRanges.fold(
          err => Async[F].raiseError(new RuntimeException(s"valid ranges parsing failed $err")),
          Async[F].pure,
        )
      } yield validRanges

    def webSocketCfg: F[WebSocketConfig] =
      for {
        webSocketCfg <- Async[F].delay {
          ConfigSource.default.at("web-socket-config").load[WebSocketConfig]
        }
        webSocketCfg <- webSocketCfg.fold(
          err => Async[F].raiseError(new RuntimeException(s"web socket config parsing failed $err")),
          Async[F].pure,
        )
      } yield webSocketCfg

    def customKafkaCfg: ConsumerConfig = {
      val kafkaConfig: Result[KafkaConfig] = ConfigSource.default.at("data-consumer-kafka-config").load[KafkaConfig]

      kafkaConfig match {
        case Right(cfg) =>
          val kafkaCommonConfig = CommonConfig.Default.copy(
            bootstrapServers = NonEmptyList.one(s"${cfg.host}:${cfg.port}"),
            clientId = Some(cfg.clientId),
          )
          ConsumerConfig.Default.copy(
            common = kafkaCommonConfig,
            groupId = Some(cfg.groupId),
            autoOffsetReset = AutoOffsetReset.Earliest,
          )
        case Left(_)    => ConsumerConfig.Default
      }
    }
  }
}
