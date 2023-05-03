package model.consumer

import java.time.Instant

sealed trait ClassifiedValue {
  val updatedOn: Instant
  val value: Int
  val status: String
}
case class CarriageSpeed(updatedOn: Instant, value: Int, status: String)  extends ClassifiedValue
case class BedTemperature(updatedOn: Instant, value: Int, status: String) extends ClassifiedValue

sealed abstract class Status(name: String)
class Valid   extends Status("valid")
class Invalid extends Status("invalid")
