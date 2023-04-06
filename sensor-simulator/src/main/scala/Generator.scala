import ConfigDomain.{ConfigPayload, Simulator, ValueRange}
import cats.effect.{Async, IO}

class Generator(simulator: IO[Simulator]) {

    trait LastValue {
      def change(delta: Int): IO[Unit]
      def get: IO[Int]
    }

  def generate(deviceType: String): IO[Int] =
    for {
      cfgPayload <- getCfgPayload(deviceType)
      newVal <- getValueInRangeWithDelta(cfgPayload.valueRange, cfgPayload.avgDelta)
    } yield newVal

  def getCfgPayload(deviceType: String): IO[ConfigPayload] =
    for {
      simulator <- simulator
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
      case "carriage-speed" => Right(simulator.carriageSpeed)
      case "bed-temp"  => Right(simulator.bedTemp)
      case _ => Left("unknown device type")
  }

  private def getValueInRangeWithDelta(range: ValueRange, delta: Int): IO[Int] =
    for {
      delta <- IO.delay { scala.util.Random.nextInt(delta) }
      lastVal <- lastValue.flatMap(_.get)
      newVal <- addOrSubtract(lastVal, delta)
      adjustedVal = adjustNumberInRange(newVal, range.min, range.max)
    } yield adjustedVal

  private def lastValue: IO[LastValue] = IO.delay {
    var lastValue = 0
    new LastValue {
      override def change(delta: Int): IO[Unit] = IO.delay(lastValue += delta)
      override def get: IO[Int] = IO.delay(lastValue)
    }
  }
}