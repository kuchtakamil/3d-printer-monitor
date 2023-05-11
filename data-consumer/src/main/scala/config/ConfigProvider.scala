package config

import cats.data.NonEmptyList
import cats.effect.IO
import com.evolutiongaming.skafka.CommonConfig
import com.evolutiongaming.skafka.consumer.{AutoOffsetReset, ConsumerConfig}
import model.config.ConsumerConfig.{KafkaConfig, ValidRanges, WebSocketConfig}
import pureconfig.ConfigReader.Result
import pureconfig.ConfigSource
import pureconfig.generic.auto._

object ConfigProvider {

  def validRanges: IO[ValidRanges] =
    for {
      validRanges <- IO.delay {
        ConfigSource.default.at("valid-ranges").load[ValidRanges]
      }
      validRanges <- validRanges.fold(
        err => IO.raiseError(new RuntimeException(s"valid ranges parsing failed $err")),
        IO.pure,
      )
    } yield validRanges

  def webSocketCfg: IO[WebSocketConfig] =
    for {
      webSocketCfg <- IO.delay {
        ConfigSource.default.at("web-socket-config").load[WebSocketConfig]
      }
      webSocketCfg <- webSocketCfg.fold(
        err => IO.raiseError(new RuntimeException(s"web socket config parsing failed $err")),
        IO.pure,
      )
    } yield webSocketCfg

  def customKafkaCfg = {
    val kafkaConfig: Result[KafkaConfig] = ConfigSource.default.at("kafka-config").load[KafkaConfig]

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
