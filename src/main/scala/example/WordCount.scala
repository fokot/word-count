package example

import zio._
import zio.console.{Console, putStrLn}
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets

import io.circe.Json
import io.circe.syntax._
import io.circe.generic.auto._
import uzhttp.Request.Method.GET
import uzhttp.server.Server
import uzhttp.Response
import zio.clock.Clock
import zio.duration._
import zio.{App, ZIO, Chunk}

import scala.io.StdIn

object WordCount extends App {

  type MapMap[A] = Map[String, Map[String, A]]
  type Counter = MapMap[Chunk[Long]]

  def mapValues[A](c: Counter, f: Chunk[Long] => A): MapMap[A] =
    c.view.mapValues(_.view.mapValues(f).toMap).toMap

  def clearOld(from: Long)(c: Counter): Counter =
    mapValues(c, _.filter(_ > from))

  def count(c: Counter): MapMap[Int] =
    mapValues(c, _.size)

  def addEvent(event: Event)(c: Counter): Counter =
    c.updatedWith(event.event_type)(x =>
      Some(x.getOrElse(Map.empty).updatedWith(event.data)(y => Some(y.getOrElse(Chunk.empty) + event.timestamp)))
    )

  case class Event(event_type: String, data: String, timestamp: Long)

  val readWriteTest: ZIO[Console, Throwable, Unit] =
    ZIO.effect(StdIn.readLine()).flatMap(
      l => ZIO.when(l != null)(putStrLn(l) *> readWriteTest)
    )

  def updateCounterFromStdin(c: Ref[Counter]): ZIO[Console with Clock, Nothing, Nothing] =
    ZIO.effect(StdIn.readLine())
      .map(io.circe.parser.parse).absolve
      .map(_.as[Event]).absolve
      .tapBoth(e => putStrLn(s"ERROR: ${e.toString}"), e => putStrLn(s"PARSED: $e"))
      .flatMap(event => c.update(addEvent(event)))
      .orElse(updateCounterFromStdin(c)) *> updateCounterFromStdin(c)

  def jsonResponse(json: Json): Response =
    Response.const(json.spaces4.getBytes(StandardCharsets.UTF_8), contentType = "application/json")

  val WINDOW = 10.seconds

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
          case req if req.method == GET && (req.uri.isOpaque || req.uri.getPath.isEmpty || req.uri.getPath == "/") =>
            c.get.map(count).map(_.asJson).map(jsonResponse)
        }.serve.useForever.orDie
    } yield res
}


