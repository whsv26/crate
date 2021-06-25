package org.whsv26.crate

import cats.data.Validated.Valid
import cats.effect.Sync
import cats.implicits._
import org.http4s.{HttpRoutes, QueryParamDecoder, QueryParameterValue}
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.QueryParamDecoderMatcher

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

  implicit val CsvQueryParamDecoder: QueryParamDecoder[Seq[Currency]]
    = (value: QueryParameterValue) => Valid(value.value.split(',').toIndexedSeq.map(Currency.withName))

  object CsvQueryParamMatcher extends QueryParamDecoderMatcher[Seq[Currency]]("currencies")

  def getCurrencyLayerRatesRoute[F[_]: Sync](CL: CurrencyLayer[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._

    HttpRoutes.of[F] {
      case GET -> Root / "currency-layer" / "rates" :? CsvQueryParamMatcher(currencies) =>
        for {
          currencyRates <- CL.getRates(currencies)
          resp <- Ok(currencyRates)
        } yield resp
    }
  }
}