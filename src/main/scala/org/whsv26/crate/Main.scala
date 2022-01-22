package org.whsv26.crate

import cats.effect.{Async, ConcurrentEffect, ExitCode, IO, IOApp, Timer, ContextShift}
import cats.implicits._
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
  val appConf: AppConfig =
    ConfigSource.resources("config/app.conf")
      .load[AppConfig]
      .toOption
      .get

  implicit def transactor[F[_]: Async: ContextShift]: Transactor[F] =
    appConf.db match {
      case PostgresConfig(host, port, user, password, database) =>
        Transactor.fromDriverManager[F](
          "org.postgresql.Driver",
          s"jdbc:postgresql://$host:$port/$database",
          user,
          password
        )
    }

  def run(args: List[String]): IO[ExitCode] =
    outgoingCurrencyRateStream[IO]
      .merge(incomingCurrencyRateStream[IO])
      .compile
      .drain
      .as(ExitCode.Success)

  private def outgoingCurrencyRateStream[F[_]: ConcurrentEffect: ContextShift: Timer] =
    CrateServer.stream[F](appConf)

  private def incomingCurrencyRateStream[F[_]: ConcurrentEffect: Timer: Transactor]: Stream[F, Int] =
    Stream.awakeEvery[F](1.hour)
      .filter { _ =>
        val now = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val dueHours = List(2, 3, 11, 12)
        dueHours.contains(now.get(Calendar.HOUR_OF_DAY))
      }
      .evalMap { _ =>
        BlazeClientBuilder[F](global).resource.use { client =>
          val currencyLayerService = CurrencyRateService[F](client, appConf)
          val currencyRateRepository = CurrencyRateRepository[F]

          val persisted = for {
            rates <- currencyLayerService.getLiveRates(Currency.nel)
            persistedQty <- currencyRateRepository.insertMany(rates)
          } yield persistedQty

          persisted.handleError(_ => 0)
        }
      }
      .evalTap(i => { println(i) }.pure[F])
}
