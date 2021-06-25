package org.whsv26.crate

import cats.effect.Sync
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object CrateRoutes {

  def getCurrencyRateRoute[F[_]: Sync](CR: CurrencyRates[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._

    HttpRoutes.of[F] {
      case GET -> Root / "currency" / "rate" =>
        for {
          currencyRate <- CR.get
          resp <- Ok(currencyRate)
        } yield resp
    }
  }

  def getCurrencyRatesRoute[F[_]: Sync](CR: CurrencyRates[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._

    HttpRoutes.of[F] {
      case GET -> Root / "currency" / "rates" =>
        for {
          currencyRates <- CR.getAll
          resp <- Ok(currencyRates)
        } yield resp
    }
  }

  def getCurrencyLayerRatesRoute[F[_]: Sync](CL: CurrencyLayer[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._

    HttpRoutes.of[F] {
      case GET -> Root / "currency-layer" / "rates" =>
        for {
          currencyRates <- CL.getRates
          resp <- Ok(currencyRates)
        } yield resp
    }
  }
}