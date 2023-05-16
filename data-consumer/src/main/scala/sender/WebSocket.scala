package sender

import cats.effect.kernel.Async
import cats.effect.Resource
import cats.syntax.all._

import scala.language.postfixOps
import com.comcast.ip4s.{Host, IpLiteralSyntax, Port}
import fs2.concurrent.Topic
import model.config.DataConsumerConfig.WebSocketConfig
import org.http4s.ember.server._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.server.Server
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame
import org.http4s._

trait WebSocket[F[_]] {
  def runWebSocketServer(websocketCfg: WebSocketConfig, topic: Topic[F, String]): Resource[F, Server]
}

object WebSocket {

  def make[F[_]: Async]: WebSocket[F] = new WebSocket[F] {
    override def runWebSocketServer(websocketCfg: WebSocketConfig, topic: Topic[F, String]): Resource[F, Server] =
      EmberServerBuilder
        .default[F]
        .withHost(Host.fromString(websocketCfg.host).getOrElse(ipv4"127.0.0.1"))
        .withPort(Port.fromString(websocketCfg.port).getOrElse(port"9000"))
        .withHttpWebSocketApp(httpApp(topic))
        .build

    private def httpApp(topic: Topic[F, String])(wsb: WebSocketBuilder2[F]): HttpApp[F] =
      HttpRoutes
        .of[F] { case GET -> Root / "simvalue" =>
          wsb.build(
            receive = (in => in.evalMap(_ => Async[F].unit)),
            send = topic.subscribe(maxQueued = 10).map(WebSocketFrame.Text(_)),
          )
        }
        .orNotFound
  }
}
