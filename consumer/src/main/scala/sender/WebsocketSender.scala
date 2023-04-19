package sender

import cats.effect.IO

class WebsocketSender {
  def send(payload: String): IO[Unit] = ???

}