package model.simulator

sealed trait SimValue {
  val updatedOn: String
  val value: Int
}
case class CarriageSpeed(updatedOn: String, value: Int)  extends SimValue
case class BedTemperature(updatedOn: String, value: Int) extends SimValue
