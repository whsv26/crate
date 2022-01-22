package org.whsv26.crate

import cats.effect.{ConcurrentEffect, ContextShift, ExitCode, IO, IOApp, Timer}
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
import org.http4s.client.Client

object Main extends IOApp {
  case class AppContext[F[_]](
    conf: AppConfig,
    xa: Transactor[F],
    client: Client[F],
    currencyRates: CurrencyRateRepository[F]
  )

  private def applicationContext[F[_]: ConcurrentEffect: ContextShift] = {
    val conf: AppConfig =
      ConfigSource.resources("config/app.conf")
        .load[AppConfig]
        .toOption
        .get

    val xa = Transactor.fromDriverManager[F](
      "org.postgresql.Driver",
      "jdbc:postgresql://%s:%s/%s".format(conf.db.host, conf.db.port, conf.db.database),
      conf.db.user,
      conf.db.password
    )

    BlazeClientBuilder[F](global).resource.map { client =>
      AppContext(
        conf = conf,
        xa = xa,
        client = client,
        currencyRates = CurrencyRateRepository[F](xa)
      )
    }
  }

  def run(args: List[String]): IO[ExitCode] = {
    val reader = for {
      out <- outgoingStream[IO]
      in <- incomingStream[IO]
      merged = out.merge(in).compile.drain.as(ExitCode.Success)
    } yield merged

    def runReader(ctx: AppContext[IO]): IO[ExitCode] = reader.run(ctx)

    applicationContext[IO].use(runReader)
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
          val currencyLayerService = CurrencyRateService[F](ctx.client, ctx.conf)

          val persisted = for {
            rates <- currencyLayerService.getLiveRates(Currency.nel)
            persistedQty <- ctx.currencyRates.insertMany(rates)
          } yield persistedQty

          persisted.handleError(_ => 0)
        }
        .evalTap(i => { println(i) }.pure[F])
    }
}
