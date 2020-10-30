package example

import zio._
import zio.console.{Console, putStrLn}

import scala.io.StdIn

object WordCount extends App {

  val rw: ZIO[Console, Throwable, Unit] =
    ZIO.effect(StdIn.readLine()).flatMap(
      l => ZIO.when(l != null)(putStrLn(l) *> rw)
    )

  def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    rw.exitCode
}