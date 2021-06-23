package org.whsv26.crate

import cats.effect.{ExitCode, IO, IOApp}
import fs2.Stream
import scala.concurrent.duration.DurationInt

object Main extends IOApp {
  def run(args: List[String]) = {
    outgoingCurrencyRateStream
      .merge(incomingCurrencyRateStream)
      .compile
      .drain
      .as(ExitCode.Success)
  }

  def outgoingCurrencyRateStream: Stream[IO, Nothing] = {
    CrateServer.stream[IO]
  }

  def incomingCurrencyRateStream: Stream[IO, Unit] = {
    Stream
      .awakeEvery[IO](5.seconds)
      .map(d => println(s"Ping! ${d.toSeconds}s"))
  }
}
