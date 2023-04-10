import ConfigDomain._
import cats.effect.std.Random
import cats.effect.{Async, IO, Ref}

class Generator(simulator: IO[Simulator], deviceType: String) {

  private val state: IO[LastValue] = initValue.flatMap(a => lastValue(a))
  def generate: IO[Int] =
    for {
      cfgPayload <- getCfgPayload
      newVal <- getValueInRangeWithDelta(cfgPayload.valueRange, cfgPayload.avgDelta)
    } yield newVal

  def getCfgPayload: IO[ConfigPayload] =
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
      case ConfigDomain.carriageSpeed => Right(simulator.carriageSpeed)
      case ConfigDomain.bedTemp  => Right(simulator.bedTemp)
      case _ => Left("unknown device type")
  }

  private def getValueInRangeWithDelta(range: ValueRange, delta: Int): IO[Int] =
    for {
      delta <-  IO.delay { scala.util.Random.nextInt(delta) }
      lastVal <- state.flatMap(_.get)
      newVal <- addOrSubtract(lastVal, delta)
      adjustedVal = adjustNumberInRange(newVal, range.min, range.max)
      _ = println(s"________________ $newVal = $lastVal $delta")
      _ <- state.flatMap(state => state.change(adjustedVal))
    } yield adjustedVal

  private trait LastValue {
    def change(delta: Int): IO[Unit]
    def get: IO[Int]
  }

  private def lastValue(initial: Int): IO[LastValue] = IO.delay {
    var lastValue = initial
    new LastValue {
      override def change(newVal: Int): IO[Unit] = IO.delay { lastValue = newVal }
      override def get: IO[Int] = IO.delay { lastValue }
    }
  }

  private def initValue =
    for {
      s <- simulator
      avg = chooseDevice(deviceType, s) match {
        case Right(v) => (v.valueRange.min + v.valueRange.max) / 2
        case Left(_) => 100
      }
    } yield avg


}