import java.time.Instant

case class CarriageSpeed(id: Id[CarriageSpeed], updatedOn: Instant, value: Int, valid: Option[Boolean])
case class BedTemperature(id: Id[BedTemperature], updatedOn: Instant, value: Int, valid: Option[Boolean])