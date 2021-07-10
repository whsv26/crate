package org.whsv26.crate

import cats.effect.{ConcurrentEffect, ExitCode, IO, IOApp}
import doobie.util.transactor.Transactor
import fs2.Stream
import org.http4s.client.blaze.BlazeClientBuilder
import org.whsv26.crate.Config.{AppConfig, PostgresConfig}
import scala.concurrent.duration.DurationInt
import pureconfig._
import pureconfig.generic.auto._
import java.util.{Calendar, TimeZone}
import scala.concurrent.ExecutionContext.global

object Main extends IOApp {
  val appConf: AppConfig = ConfigSource.resources("config/app.conf").load[AppConfig].toOption.get

  implicit val xa: Transactor.Aux[IO, Unit] = appConf.db match {
    case PostgresConfig(host, port, user, password, database) => Transactor.fromDriverManager[IO](
      "org.postgresql.Driver",
      s"jdbc:postgresql://$host:$port/$database",
      user,
      password
    )
  }

  def run(args: List[String]): IO[ExitCode] = {
    outgoingCurrencyRateStream
      .merge(incomingCurrencyRateStream)
      .compile
      .drain
      .as(ExitCode.Success)
  }

  def outgoingCurrencyRateStream: Stream[IO, Nothing] = {
    CrateServer.stream[IO](appConf)
  }

  def incomingCurrencyRateStream(implicit xa: Transactor.Aux[IO, Unit], ce: ConcurrentEffect[IO]): Stream[IO, Int] = {
    Stream
      .awakeEvery[IO](1.hour)
      .filter { _ =>
        val now = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val dueHours = List(2, 3)
        dueHours.contains(now.get(Calendar.HOUR_OF_DAY))
      }
      .evalMap(_ => {
        BlazeClientBuilder[IO](global).resource.use { client =>
          val currencyLayerService = CurrencyLayerService.impl[IO](client, appConf)
          val currencyRateRepository = CurrencyRateRepository.impl[IO](xa)
          val persisted = for {
            rates <- currencyLayerService.getLiveRates(Currency.nel)
            persistedQty <- currencyRateRepository.insertMany(rates)
          } yield persistedQty
          persisted.handleErrorWith(_ => IO(0))
        }.to
      })
      .evalTap(i => IO(println(i)))
  }
}
