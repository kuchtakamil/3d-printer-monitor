package model.simulator

import java.time.Instant

sealed trait SimValue {
  val updatedOn: Instant
  val value: Int
}
case class CarriageSpeed(updatedOn: Instant, value: Int)  extends SimValue
case class BedTemperature(updatedOn: Instant, value: Int) extends SimValue
