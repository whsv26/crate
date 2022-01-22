package org.whsv26.crate

import cats.effect.{ConcurrentEffect, Timer}
import doobie.util.transactor.Transactor
import fs2.Stream
import org.http4s.HttpRoutes
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import org.whsv26.crate.Config.AppConfig

import scala.concurrent.ExecutionContext.global

object CrateServer {

  def stream[F[_]: ConcurrentEffect: Timer](conf: AppConfig)(implicit
    xa: Transactor.Aux[F, Unit]
  ): Stream[F, Nothing] = {

    val currencyRateRepository = CurrencyRateRepository.impl[F](xa)

    def middleware(route: HttpRoutes[F]) =
      AccessKeyMiddleware(route, conf)

    // Combine Service Routes into an HttpApp.
    // Can also be done via a Router if you
    // want to extract a segments not checked
    // in the underlying routes.
    val httpApp = (
      middleware(CrateRoutes.getLiveCurrencyRates[F](currencyRateRepository))
    ).orNotFound

    // With Middlewares in place
    val finalHttpApp = Logger.httpApp(logHeaders = true, logBody = true)(httpApp)

    for {
      exitCode <- BlazeServerBuilder[F](global)
        .bindHttp(conf.server.port, conf.server.host)
        .withHttpApp(finalHttpApp)
        .serve
    } yield exitCode

  }.drain
}
