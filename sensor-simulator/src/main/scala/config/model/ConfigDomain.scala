package config.model

object ConfigDomain {
  val carriageSpeed = "carriage-speed"
  val bedTemp = "bed-temp"
  case class ConfigPayload(valueRange: ValueRange, validValueRange: ValueRange, initValue: Int, frequency: Int, avgDelta: Int)
  case class ValueRange(min: Int, max: Int)
  case class Simulator(carriageSpeed: ConfigPayload, bedTemp: ConfigPayload)
}