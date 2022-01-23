package org.whsv26.crate

import cats.effect.{Async, BracketThrow, ConcurrentEffect, ContextShift, Resource}
import cats.implicits._
import doobie.util.transactor.Transactor
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.whsv26.crate.Config.{AppConfig, PostgresConfig}
import pureconfig._
import pureconfig.error.ConfigReaderFailures
import pureconfig.generic.auto._
import scala.concurrent.ExecutionContext.global

case class AppContext[F[_]](
  conf: AppConfig,
  xa: Transactor[F],
  client: Client[F],
  repository: CurrencyRateRepository[F],
  service: CurrencyRateService[F]
)

object AppContext {
  def apply[F[_]: ContextShift: ConcurrentEffect]: Resource[F, AppContext[F]] =
    for {
      cf <- conf[F]
      xa <- transactor[F](cf.db)
      cl <- BlazeClientBuilder[F](global).resource
      crr = CurrencyRateRepository[F](xa)
      crs = CurrencyRateService[F](cl, cf)
    } yield AppContext[F](
      conf = cf,
      xa = xa,
      client = cl,
      repository = crr,
      service = crs
    )

  case class ConfigParsingError(err: ConfigReaderFailures) extends Throwable

  def conf[F[_]: BracketThrow]: Resource[F, AppConfig] = {
    Resource.eval {
      ConfigSource.resources("config/app.conf")
        .load[AppConfig]
        .leftMap(ConfigParsingError)
        .liftTo[F]
    }
  }

  def transactor[F[_]: Async: ContextShift](db: PostgresConfig): Resource[F, Transactor[F]] = {
    Resource.pure[F, Transactor[F]] {
      Transactor.fromDriverManager[F](
        "org.postgresql.Driver",
        "jdbc:postgresql://%s:%s/%s".format(db.host, db.port, db.database),
        db.user,
        db.password
      )
    }
  }
}
