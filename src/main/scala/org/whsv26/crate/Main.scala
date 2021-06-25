package org.whsv26.crate

import cats.effect.{ExitCode, IO, IOApp}
import doobie.util.transactor.Transactor
import fs2.Stream
import org.whsv26.crate.Config.PostgresConfig
import scala.concurrent.duration.DurationInt
import pureconfig._
import pureconfig.generic.auto._

object Main extends IOApp {
  val pgConf: PostgresConfig = ConfigSource
    .resources("config/database.conf")
    .load[PostgresConfig]
    .toOption
    .get

  implicit val xa: Transactor.Aux[IO, Unit] = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    s"jdbc:postgresql://${pgConf.host}:${pgConf.port}/${pgConf.database}",
    pgConf.user,
    pgConf.password
  )

  def run(args: List[String]): IO[ExitCode] = {
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
