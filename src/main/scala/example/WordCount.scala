package example

import zio._
import zio.console.{Console, putStrLn}
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets

import io.circe.{Encoder, Json}
import io.circe.syntax._
import io.circe.generic.auto._
import uzhttp.Request.Method.GET
import uzhttp.server.Server
import uzhttp.Response
import zio.clock.Clock
import zio.duration._
import zio.{App, Chunk, ZIO}

import scala.io.StdIn

object WordCount extends App {

  type Counter = Map[String, Map[String, Chunk[Long]]]

  def clearOld(from: Long)(c: Counter): Counter =
    c.view.mapValues(_.view.mapValues(_.filter(_ > from)).toMap).toMap

  def addEvent(event: Event)(c: Counter): Counter =
    c.updatedWith(event.event_type)(x =>
      Some(x.getOrElse(Map.empty).updatedWith(event.data)(y => Some(y.getOrElse(Chunk.empty) + event.timestamp)))
    )

  case class Event(event_type: String, data: String, timestamp: Long)

  def updateCounterFromStdin(c: Ref[Counter]): ZIO[Console with Clock, Nothing, Nothing] =
    ZIO.effect(StdIn.readLine())
      .map(io.circe.parser.decode[Event]).absolve
      .tapBoth(e => putStrLn(s"ERROR: ${e.toString}"), e => putStrLn(s"PARSED: $e"))
      .flatMap(event => c.update(addEvent(event)))
      .orElse(updateCounterFromStdin(c)) *> updateCounterFromStdin(c)

  def jsonResponse(json: Json): Response =
    Response.const(json.spaces4.getBytes(StandardCharsets.UTF_8), contentType = "application/json")

  val WINDOW = 10.seconds

  implicit val chunkLongEncoder: Encoder[Chunk[Long]] = Encoder.instance(_.size.asJson)

  def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
  for {
      c <- Ref.make[Counter](Map.empty)
      _ <- updateCounterFromStdin(c).fork
      _ <- clock.instant
        .flatMap(x => c.update(clearOld(x.toEpochMilli)))
        .schedule(Schedule.linear(WINDOW))
        .fork
      res <- Server.builder(new InetSocketAddress("0.0.0.0", 8080))
        .handleSome {
          case req if req.method == GET && Some(req.uri.getPath).forall(_ == "/") =>
            c.get.map(_.asJson).map(jsonResponse)
        }.serve.useForever.orDie
    } yield res
}


