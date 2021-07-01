package org.whsv26.crate

import cats.effect.{ConcurrentEffect, ExitCode, IO, IOApp}
import doobie.util.transactor.Transactor
import fs2.Stream
import org.http4s.client.blaze.BlazeClientBuilder
import org.whsv26.crate.Config.{AppConfig, PostgresConfig}
import org.whsv26.crate.Currency.{HUF, RON}

import scala.concurrent.duration.DurationInt
import pureconfig._
import pureconfig.generic.auto._
import scala.concurrent.ExecutionContext.global

object Main extends IOApp {
  val appConf: AppConfig =
    ConfigSource.resources("config/app.conf").load[AppConfig].toOption.get
  val pgConf: PostgresConfig = appConf.db

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

  def incomingCurrencyRateStream(implicit xa: Transactor.Aux[IO, Unit], ce: ConcurrentEffect[IO]): Stream[IO, Int] = {
    Stream
      .awakeEvery[IO](1.minute)
      .evalMap(_ => {
        BlazeClientBuilder[IO](global).resource.use { client =>
          val currencyLayerService = CurrencyLayerService.impl[IO](client)
          val currencyRateRepository = CurrencyRateRepository.impl[IO](xa)
          val persisted = for {
            rates <- currencyLayerService.getLiveRates(List(RON, HUF)) // TODO add full list of currencies
            persistedQty <- currencyRateRepository.insertMany(rates)
          } yield persistedQty
          persisted.handleErrorWith(_ => IO(0))
        }.to
      })
      .evalTap(i => IO(println(i)))
  }

}
