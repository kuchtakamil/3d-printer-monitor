package generator
import cats.syntax.all._
import cats.effect.kernel.Async
import cats.effect.Ref
import config.ConfigProvider
import model.config.SimulatorConfig
import model.config.SimulatorConfig._

trait Generator[F[_]] {
  def generate: F[Int]
}

object Generator {
  def of[F[_]: Async](
    simulator: Simulator,
    configProvider: ConfigProvider[F],
    deviceType: String,
    state: Ref[F, Int],
  ): Generator[F] = new Generator[F] {
    def generate: F[Int] =
      for {
        cfgPayload <- configProvider.cfgPayload(simulator, deviceType)
        newVal     <- getValueInRangeWithDelta(cfgPayload.valueRange, cfgPayload.avgDelta)
      } yield newVal

    private def addOrSubtract(a: Int, b: Int): F[Int] =
      Async[F]
        .delay {
          scala.util.Random.nextBoolean()
        }
        .map(r => if (r) a + b else a - b)

    private def adjustNumberInRange(num: Int, min: Int, max: Int): Int = {
      if (num < min) min else if (num > max) max else num
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
