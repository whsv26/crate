package org.whsv26.crate

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.implicits._
import org.http4s.{HttpRoutes, ParseFailure, QueryParamDecoder, QueryParameterValue}
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.QueryParamDecoderMatcher

object CrateRoutes {
  implicit val csvCurrencyQueryParamDecoder: QueryParamDecoder[NonEmptyList[Currency]] = (value: QueryParameterValue) => {
    val list = value
      .value
      .split(',')
      .toList
      .filter(Currency.set)
      .map(Currency.withName)

    NonEmptyList.fromList(list).toValidNel(ParseFailure("at least one element must be present", ""))
  }

  object CurrenciesParamMatcher extends QueryParamDecoderMatcher[NonEmptyList[Currency]]("currencies")

  def getLiveCurrencyRates[F[_]: Sync](repo: CurrencyRateRepository[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._

    HttpRoutes.of[F] {
      case GET -> Root / "api" / "live" :? CurrenciesParamMatcher(currencies) =>
        for {
          currencyRates <- repo.findByCurrencies(currencies)
          resp <- Ok(currencyRates)
        } yield resp
    }
  }
}