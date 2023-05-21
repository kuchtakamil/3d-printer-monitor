package config

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import model.config.SimulatorConfig.{ConfigPayload, Simulator, ValueRange}
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.language.postfixOps

class ConfigProviderSpec extends AsyncFlatSpec with Matchers {

  "ConfigProvider" should "load custom Kafka config" in {
    val configProvider = ConfigProvider.make[IO]
    val kafkaConfig    = configProvider.customKafkaCfg

    kafkaConfig.common.bootstrapServers.head shouldBe "kafka:9093"
    kafkaConfig.common.bootstrapServers.head shouldBe "kafka:9093"
  }

  it should "load simulator config" in {
    val configProvider       = ConfigProvider.make[IO]
    val simulator: Simulator = configProvider.simulator.unsafeRunSync()

    simulator.carriageSpeed.avgDelta shouldBe a[Int]
    simulator.carriageSpeed.frequency shouldBe a[Int]
    simulator.carriageSpeed.valueRange shouldBe a[ValueRange]
  }

  it should "return config payload for a known device type" in {
    val configProvider = ConfigProvider.make[IO]
    val simulator      = Simulator(
      carriageSpeed = ConfigPayload(ValueRange(40, 150), 60, 2, 4),
      bedTemp = ConfigPayload(ValueRange(60, 160), 80, 3, 6),
    )
    val cfgPayload     = configProvider
      .cfgPayload(simulator, "bed-temperature")
      .unsafeRunSync()

    cfgPayload shouldBe ConfigPayload(ValueRange(60, 160), 80, 3, 6)
  }

  it should "raise an error for an unknown device type" in {
    val configProvider = ConfigProvider.make[IO]
    val simulator      = Simulator(
      carriageSpeed = ConfigPayload(ValueRange(40, 150), 60, 2, 4),
      bedTemp = ConfigPayload(ValueRange(60, 160), 80, 3, 6),
    )

    val result = configProvider.cfgPayload(simulator, "unknownDevice").attempt.unsafeRunSync()

    result.isLeft shouldBe true
  }
}
