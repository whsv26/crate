package org.whsv26.crate

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.implicits._
import org.http4s.{HttpRoutes, ParseFailure, QueryParamDecoder, QueryParameterValue}
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.QueryParamDecoderMatcher

object CrateRoutes {
  implicit val decoder: QueryParamDecoder[NonEmptyList[Currency]] =
    (_: QueryParameterValue)
      .value
      .split(',')
      .toList
      .filter(Currency.set)
      .map(Currency.withName)
      .toNel
      .toValidNel(ParseFailure("At least one currency required", ""))

  object Matcher extends
    QueryParamDecoderMatcher[NonEmptyList[Currency]]("currencies")

  def getLiveCurrencyRates[F[_]: Sync](
    repo: CurrencyRateRepository[F]
  ): HttpRoutes[F] = {

    val dsl = new Http4sDsl[F]{}
    import dsl._

    HttpRoutes.of[F] {
      case GET -> Root / "api" / "live" :? Matcher(currencies) =>
        for {
          currencyRates <- repo.findByCurrencies(currencies)
          resp <- Ok(currencyRates)
        } yield resp
    }
  }
}