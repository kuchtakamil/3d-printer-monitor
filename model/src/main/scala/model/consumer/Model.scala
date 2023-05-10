package model.consumer

sealed trait ClassifiedValue {
  val updatedOn: String
  val value: Int
  val status: String
}
case class CarriageSpeed(updatedOn: String, value: Int, status: String)  extends ClassifiedValue
case class BedTemperature(updatedOn: String, value: Int, status: String) extends ClassifiedValue

sealed abstract class Status(name: String)
class Valid   extends Status("valid")
class Invalid extends Status("invalid")
