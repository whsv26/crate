package org.whsv26.crate

import cats.effect.Sync
import doobie.{Read, Write}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import org.http4s.{EntityDecoder, EntityEncoder}
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import java.time.Instant
import Syntax._

final case class CurrencyRate(currency: Currency, rate: Double, actualAt: Instant)

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
    Read[(String, Double, String)].map { case (c, r, aa) => CurrencyRate(Currency.withName(c), r, aa.toInstant) }
  implicit val currencyRateWrite: Write[CurrencyRate] =
    Write[(String, Double, String)].contramap(p => (p.currency.toString, p.rate, p.actualAt.toTimeStampString))
}
