import ConfigDomain.{ConfigPayload, Simulator, ValueRange}
import cats.effect.{Async, IO}
import pureconfig._
import pureconfig.generic.auto._

object Generator {

  var lastVal: Int = 0

  def generate(deviceType: String): IO[Int] = {
    for {
      simulator <- IO.delay { ConfigSource.default.at("simulator").load[Simulator] }
      simulator <- simulator.fold(
        err => IO.raiseError(new RuntimeException(s"parsing failed $err")),
        IO.pure)
      cfgPayload <- chooseDevice(deviceType, simulator) match {
        case Right(v) => IO.pure(v)
        case Left(e) => IO.raiseError(new RuntimeException(e))
      }
      newVal <- getValueInRangeWithDelta(cfgPayload.valueRange, cfgPayload.avgDelta)
    } yield newVal
  }

  //  }
  //  case class RandomOp(a: Int, b: Int) {
  //    def apply: Int = if (scala.util.Random.nextBoolean()) a + b else a - b

  private def addOrSubtract(a: Int, b: Int): IO[Int] = {
    IO.delay { scala.util.Random.nextBoolean() }
      .map(r => if (r) a + b else a - b)
  }

  private def adjustNumberInRange(num: Int, min: Int, max: Int): Int = {
    if (num < min) min else if (num > max) max else num
  }

  private def chooseDevice(deviceType: String, simulator: Simulator): Either[String, ConfigPayload] = deviceType match {
    case "carriage-speed" => Right(simulator.carriageSpeed)
    case "bed-temp"  => Right(simulator.bedTemp)
    case _ => Left("unknown device type")
  }

  private def getValueInRangeWithDelta(range: ValueRange, delta: Int): IO[Int] = {
    for {
      delta <- IO.delay { scala.util.Random.nextInt(delta) }
      v <- addOrSubtract(lastVal, delta)
      adjustedVal = adjustNumberInRange(v, range.min, range.max)
    } yield adjustedVal
  }

}
