package org.whsv26.crate

import cats.effect.Sync
import cats.implicits._
import io.circe._
import io.circe.generic.semiauto._
import org.http4s._
import org.http4s.circe._
import org.whsv26.crate.Currency.RUB
import doobie._
import doobie.implicits._
import org.whsv26.crate.Syntax._
import java.time.Instant

trait CurrencyRates[F[_]]{
  def get: F[CurrencyRates.CurrencyRate]
  def getAll: F[List[CurrencyRates.CurrencyRate]]
}

object CurrencyRates {
  def apply[F[_]](implicit ev: CurrencyRates[F]): CurrencyRates[F] = ev

  final case class CurrencyRate(currency: Currency, rate: Double, actualAt: Instant)
  final case class CurrencyRateError(e: Throwable) extends RuntimeException

  object CurrencyRate {
    // Circe Encoder/Decoder
    implicit val currencyRateDecoder: Decoder[CurrencyRate] = deriveDecoder[CurrencyRate]
    implicit def currencyRateEntityDecoder[F[_]: Sync]: EntityDecoder[F, CurrencyRate] = jsonOf

    implicit val currencyRateEncoder: Encoder[CurrencyRate] = deriveEncoder[CurrencyRate]
    implicit def currencyRateEntityEncoder[F[_]]: EntityEncoder[F, CurrencyRate] = jsonEncoderOf

    implicit val currencyRateListEncoder: Encoder[List[CurrencyRate]] = deriveEncoder[List[CurrencyRate]]
    implicit def currencyRateListEntityEncoder[F[_]]: EntityEncoder[F, List[CurrencyRate]] = jsonEncoderOf

    // Doobie Read/Write
    implicit val currencyRateRead: Read[CurrencyRate] =
      Read[(String, Double, String)].map { case (c, r, aa) => CurrencyRate(Currency.withName(c), r, Instant.parse(aa)) }
    implicit val currencyRateWrite: Write[CurrencyRate] =
      Write[(String, Double, String)].contramap(p => (p.currency.toString, p.rate, p.actualAt.toTimeStampString))
  }

  def impl[F[_]: Sync](xa: Transactor.Aux[F, Unit]): CurrencyRates[F] = new CurrencyRates[F]{
    def get: F[CurrencyRate] = {
      CurrencyRate(RUB, 75.01, Instant.now()).pure[F]
    }
    def getAll: F[List[CurrencyRate]] = {
      sql"SELECT currency, rate, actual_at FROM currency_rates"
        .query[CurrencyRate]
        .stream
        .compile
        .toList
        .transact(xa)
    }
  }
}


