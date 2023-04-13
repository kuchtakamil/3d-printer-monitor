import java.time.Instant

sealed trait SimValue
case class CarriageSpeed(updatedOn: Instant, value: Int) extends SimValue
case class BedTemperature(updatedOn: Instant, value: Int) extends SimValue



