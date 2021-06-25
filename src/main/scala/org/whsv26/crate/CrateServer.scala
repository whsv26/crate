package org.whsv26.crate

import cats.effect.{ConcurrentEffect, Timer}
import cats.implicits._
import doobie.util.transactor.Transactor.Aux
import fs2.Stream
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger

import scala.concurrent.ExecutionContext.global

object CrateServer {

  def stream[F[_]: ConcurrentEffect](implicit T: Timer[F], xa: Aux[F, Unit]): Stream[F, Nothing] = {
    for {
      client <- BlazeClientBuilder[F](global).stream

      // Implementations
      currencyRateAlg = CurrencyRates.impl[F](xa)
      currencyLayerAlg = CurrencyLayer.impl[F](client)

      // Combine Service Routes into an HttpApp.
      // Can also be done via a Router if you
      // want to extract a segments not checked
      // in the underlying routes.
      httpApp = (
        CrateRoutes.getCurrencyRateRoute[F](currencyRateAlg) <+>
        CrateRoutes.getCurrencyRatesRoute[F](currencyRateAlg) <+>
        CrateRoutes.getCurrencyLayerRatesRoute[F](currencyLayerAlg)
      ).orNotFound

      // With Middlewares in place
      finalHttpApp = Logger.httpApp(true, true)(httpApp)

      exitCode <- BlazeServerBuilder[F](global)
        .bindHttp(8080, "localhost")
        .withHttpApp(finalHttpApp)
        .serve
    } yield exitCode
  }.drain
}
