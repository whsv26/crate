package org.whsv26.crate

import cats.effect.{ConcurrentEffect, Timer}
import fs2.Stream
import org.http4s.HttpRoutes
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import scala.concurrent.ExecutionContext.global

object CrateServer {

  def stream[F[_]: ConcurrentEffect: Timer](
    ctx: AppContext[F]
  ): Stream[F, Nothing] = {

    def middleware(route: HttpRoutes[F]) =
      AuthMiddleware(route, ctx.conf)

    val currencyRateRepository = CurrencyRateRepository[F](ctx.xa)

    // Combine Service Routes into an HttpApp.
    // Can also be done via a Router if you
    // want to extract a segments not checked
    // in the underlying routes.
    val httpApp = (
      middleware(CrateRoutes.getLiveCurrencyRates[F](currencyRateRepository))
    ).orNotFound

    // With Middlewares in place
    val finalHttpApp = Logger.httpApp(logHeaders = true, logBody = true)(httpApp)

    val serverConf = ctx.conf.server

    for {
      exitCode <- BlazeServerBuilder[F](global)
        .bindHttp(serverConf.port, serverConf.host)
        .withHttpApp(finalHttpApp)
        .serve
    } yield exitCode

  }.drain
}
