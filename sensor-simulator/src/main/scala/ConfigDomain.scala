object ConfigDomain {
  case class ConfigPayload(valueRange: ValueRange, validValueRange: ValueRange, frequency: Int, avgDelta: Int)
  case class ValueRange(min: Int, max: Int)
  case class Simulator(carriageSpeed: ConfigPayload, bedTemp: ConfigPayload)
}