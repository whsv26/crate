package org.whsv26.crate

import cats.effect.{ConcurrentEffect, ExitCode, IO, IOApp}
import doobie.util.transactor.Transactor
import doobie.util.transactor.Transactor.Aux
import fs2.Stream
import org.http4s.client.blaze.BlazeClientBuilder
import org.whsv26.crate.Config.PostgresConfig
import org.whsv26.crate.Currency.{HUF, RON}
import scala.concurrent.duration.DurationInt
import pureconfig._
import pureconfig.generic.auto._
import scala.concurrent.ExecutionContext.global

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

  def incomingCurrencyRateStream(implicit xa: Aux[IO, Unit], ce: ConcurrentEffect[IO]): Stream[IO, Int] = {
    Stream
      .awakeEvery[IO](1.minute)
      .evalMap(_ => {
        BlazeClientBuilder[IO](global).resource.use { client =>
          val currencyLayerAlg = CurrencyLayer.impl[IO](client, xa)
          val persisted = for {
            rates <- currencyLayerAlg.getCurrentRates(List(RON, HUF))
            persistedQty <- currencyLayerAlg.persistCurrencyRates(rates)
          } yield persistedQty
          persisted.handleErrorWith(_ => IO(0))
        }.to
      })
      .evalTap(i => IO(println(i)))
  }

}
