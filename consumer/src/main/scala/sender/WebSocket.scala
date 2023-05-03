package sender

import cats.effect.{IO, Resource}

import scala.language.postfixOps
import com.comcast.ip4s.{Host, IpLiteralSyntax, Port}
import fs2.concurrent.Topic
import fs2.Stream
import model.config.ConsumerConfig.WebSocketConfig
import org.http4s.ember.server._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.server.Server
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame
import org.http4s._

object WebSocket {
  def runWebSocketServer(websocketCfg: WebSocketConfig, topic: Topic[IO, String]): Resource[IO, Server] =
    EmberServerBuilder
      .default[IO]
      .withHost(Host.fromString(websocketCfg.host).getOrElse(ipv4"127.0.0.1"))
      .withPort(Port.fromString(websocketCfg.port).getOrElse(port"9000"))
      .withHttpWebSocketApp(httpApp(topic, websocketCfg.websocketEndpoint))
      .build

  private def httpApp(topic: Topic[IO, String], websocketEndpoint: String)(wsb: WebSocketBuilder2[IO]): HttpApp[IO] =
    HttpRoutes
      .of[IO] { case GET -> Root / "simvalue" =>
        wsb.build(
          receive = topic.publish.compose[Stream[IO, WebSocketFrame]](_.collect {
            case WebSocketFrame.Text(message, _) => message
          }),
          send = topic.subscribe(maxQueued = 10).map(WebSocketFrame.Text(_)),
        )
      }
      .orNotFound
}
