package model.config

object ConsumerConfig {
  val carriageSpeed = "carriage-speed"
  val bedTemp       = "bed-temp"
  case class WebSocketConfig(host: String, port: String)

  case class KafkaConfig(host: String, port: String, clientId: String, groupId: String)

  case class ValidRanges(carriageSpeed: CarriageSpeed, bedTemp: BedTemp)
  case class CarriageSpeed(validValueRange: ValidValueRange)
  case class BedTemp(validValueRange: ValidValueRange)
  case class ValidValueRange(min: Int, max: Int)
}
