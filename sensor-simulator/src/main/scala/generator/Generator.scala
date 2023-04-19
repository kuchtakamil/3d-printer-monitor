package generator

import cats.effect.{IO, Ref}
import config.model.ConfigDomain
import config.model.ConfigDomain.{ConfigPayload, Simulator, ValueRange}

class Generator(simulator: Simulator, deviceType: String, state: Ref[IO, Int]) {

  def generate: IO[Int] =
    for {
      cfgPayload <- getCfgPayload
      newVal <- getValueInRangeWithDelta(cfgPayload.valueRange, cfgPayload.avgDelta)
    } yield newVal

  def getCfgPayload: IO[ConfigPayload] =
    for {
      cfgPayload <- chooseDevice(deviceType, simulator) match {
        case Right(v) => IO.pure(v)
        case Left(e) => IO.raiseError(new RuntimeException(e))
      }
    } yield cfgPayload

  private def addOrSubtract(a: Int, b: Int): IO[Int] =
    IO.delay { scala.util.Random.nextBoolean() }
      .map(r => if (r) a + b else a - b)

  private def adjustNumberInRange(num: Int, min: Int, max: Int): Int = {
    if (num < min) min else if (num > max) max else num
  }

  private def chooseDevice(deviceType: String, simulator: Simulator): Either[String, ConfigPayload] =
    deviceType match {
      case ConfigDomain.carriageSpeed => Right(simulator.carriageSpeed)
      case ConfigDomain.bedTemp  => Right(simulator.bedTemp)
      case _ => Left("unknown device type")
    }

  private def getValueInRangeWithDelta(range: ValueRange, delta: Int): IO[Int] =
    for {
      delta <-  IO.delay { scala.util.Random.nextInt(delta) }
      lastVal <- state.get
      newVal <- addOrSubtract(lastVal, delta)
      adjustedVal = adjustNumberInRange(newVal, range.min, range.max)
      _ <- state.set(adjustedVal)
    } yield adjustedVal
}