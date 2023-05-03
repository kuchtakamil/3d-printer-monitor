package generator
import cats.syntax.all._
import cats.Monad
import cats.effect.kernel.Async
import cats.effect.{IO, Ref}
import model.config.SimulatorConfig
import model.config.SimulatorConfig._

trait Generator[F[_]] {
  def generate: F[Int]
  def getCfgPayload: F[ConfigPayload]
}

object Generator {
  def of[F[_]: Async](simulator: Simulator, deviceType: String, state: Ref[F, Int]) = new Generator[F] {
    def generate: F[Int] =
      for {
        cfgPayload <- getCfgPayload
        newVal     <- getValueInRangeWithDelta(cfgPayload.valueRange, cfgPayload.avgDelta)
      } yield newVal

    def getCfgPayload: F[ConfigPayload] =
      chooseDevice(deviceType, simulator) match {
        case Right(v) => Async[F].pure(v)
        case Left(e)  => Async[F].raiseError(new RuntimeException(e))
      }

    private def addOrSubtract(a: Int, b: Int): F[Int] =
      Async[F]
        .delay {
          scala.util.Random.nextBoolean()
        }
        .map(r => if (r) a + b else a - b)

    private def adjustNumberInRange(num: Int, min: Int, max: Int): Int = {
      if (num < min) min else if (num > max) max else num
    }

    private def chooseDevice(deviceType: String, simulator: Simulator): Either[String, ConfigPayload] =
      deviceType match {
        case SimulatorConfig.carriageSpeed => Right(simulator.carriageSpeed)
        case SimulatorConfig.bedTemp       => Right(simulator.bedTemp)
        case _                             => Left("unknown device type")
      }

    private def getValueInRangeWithDelta(range: ValueRange, delta: Int): F[Int] =
      for {
        delta      <- Async[F].delay {
          scala.util.Random.nextInt(delta)
        }
        lastVal    <- state.get
        newVal     <- addOrSubtract(lastVal, delta)
        adjustedVal = adjustNumberInRange(newVal, range.min, range.max)
        _          <- state.set(adjustedVal)
      } yield adjustedVal
  }
}

class Generator1[F[_]: Async](simulator: Simulator, deviceType: String, state: Ref[F, Int]) {

  def generate: F[Int] =
    for {
      cfgPayload <- getCfgPayload
      newVal     <- getValueInRangeWithDelta(cfgPayload.valueRange, cfgPayload.avgDelta)
    } yield newVal

  def getCfgPayload: F[ConfigPayload] =
    chooseDevice(deviceType, simulator) match {
      case Right(v) => Async[F].pure(v)
      case Left(e)  => Async[F].raiseError(new RuntimeException(e))
    }

  private def addOrSubtract(a: Int, b: Int): F[Int] =
    Async[F]
      .delay { scala.util.Random.nextBoolean() }
      .map(r => if (r) a + b else a - b)

  private def adjustNumberInRange(num: Int, min: Int, max: Int): Int = {
    if (num < min) min else if (num > max) max else num
  }

  private def chooseDevice(deviceType: String, simulator: Simulator): Either[String, ConfigPayload] =
    deviceType match {
      case SimulatorConfig.carriageSpeed => Right(simulator.carriageSpeed)
      case SimulatorConfig.bedTemp       => Right(simulator.bedTemp)
      case _                             => Left("unknown device type")
    }

  private def getValueInRangeWithDelta(range: ValueRange, delta: Int): F[Int] =
    for {
      delta      <- Async[F].delay { scala.util.Random.nextInt(delta) }
      lastVal    <- state.get
      newVal     <- addOrSubtract(lastVal, delta)
      adjustedVal = adjustNumberInRange(newVal, range.min, range.max)
      _          <- state.set(adjustedVal)
    } yield adjustedVal
}
