package model.config

object ConsumerConfig {
  val carriageSpeed = "carriage-speed"
  val bedTemp = "bed-temp"
  case class WebApp(host: String, port: String)
}