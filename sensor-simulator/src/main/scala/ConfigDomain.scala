object ConfigDomain {
  case class ConfigPayload(valueRange: ValueRange, validValueRange: ValueRange, everyNSec: Int, avgDelta: Int)
  case class ValueRange(min: Int, max: Int)
  case class Simulator(carriageSpeed: ConfigPayload, bedTemp: ConfigPayload)
}