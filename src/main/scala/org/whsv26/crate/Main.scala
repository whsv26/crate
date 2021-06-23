package org.whsv26.crate

import cats.effect.{ExitCode, IO, IOApp}
import doobie.util.transactor.Transactor
import fs2.Stream
import scala.concurrent.duration.DurationInt

object Main extends IOApp {
  implicit val xa: Transactor.Aux[IO, Unit] = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql://localhost:54325/crate",
    "docker",
    "docker"
  )

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
      .awakeEvery[IO](30.seconds)
      .map(d => println(s"Ping! ${d.toSeconds}s"))
  }
}
