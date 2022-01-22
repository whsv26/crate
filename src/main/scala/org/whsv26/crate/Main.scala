package org.whsv26.crate

import cats.effect.{ConcurrentEffect, ExitCode, IO, IOApp, Timer}
import cats.implicits._
import doobie.util.transactor.Transactor
import fs2.Stream
import org.http4s.client.blaze.BlazeClientBuilder
import org.whsv26.crate.Config.AppConfig
import scala.concurrent.duration.DurationInt
import pureconfig._
import pureconfig.generic.auto._
import java.util.{Calendar, TimeZone}
import scala.concurrent.ExecutionContext.global
import cats.data.Reader

object Main extends IOApp {
  case class AppContext[F[_]](conf: AppConfig, xa: Transactor[F])

  private lazy val applicationContext = {
    val conf: AppConfig =
      ConfigSource.resources("config/app.conf")
        .load[AppConfig]
        .toOption
        .get

    val xa = Transactor.fromDriverManager[IO](
      "org.postgresql.Driver",
      "jdbc:postgresql://%s:%s/%s".format(conf.db.host, conf.db.port, conf.db.database),
      conf.db.user,
      conf.db.password
    )

    AppContext(conf, xa)
  }

  def run(args: List[String]): IO[ExitCode] = {
    val reader = for {
      out <- outgoingStream[IO]
      in <- incomingStream[IO]
      merged = out.merge(in).compile.drain.as(ExitCode.Success)
    } yield merged

    reader.run(applicationContext)
  }

  private def outgoingStream[F[_]: ConcurrentEffect: Timer]: Reader[AppContext[F], Stream[F, Nothing]] =
    Reader { CrateServer.stream[F](_) }

  private def incomingStream[F[_]: ConcurrentEffect: Timer]: Reader[AppContext[F], Stream[F, Int]] =
    Reader { ctx =>
      Stream
        .awakeEvery[F](1.hour)
        .filter { _ =>
          val now = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
          val dueHours = List(2, 3, 11, 12)
          dueHours.contains(now.get(Calendar.HOUR_OF_DAY))
        }
        .evalMap { _ =>
          BlazeClientBuilder[F](global).resource.use { client =>
            val currencyLayerService = CurrencyRateService[F](client, ctx.conf)
            val currencyRateRepository = CurrencyRateRepository[F](ctx.xa)

            val persisted = for {
              rates <- currencyLayerService.getLiveRates(Currency.nel)
              persistedQty <- currencyRateRepository.insertMany(rates)
            } yield persistedQty

            persisted.handleError(_ => 0)
          }
        }
        .evalTap(i => { println(i) }.pure[F])
    }
}
