package org.whsv26.crate

import cats.effect.Sync
import doobie.{Read, Write}
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import org.http4s.{EntityDecoder, EntityEncoder}
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import io.circe.syntax._
import java.time.Instant
import Syntax._

final case class CurrencyRate(
  currency: Currency,
  rate: Double,
  actualAt: Instant
)

object CurrencyRate {
  // Circe Encoder/Decoder
  implicit val decoder: Decoder[CurrencyRate] = deriveDecoder[CurrencyRate]
  implicit def entityDecoder[F[_]: Sync]: EntityDecoder[F, CurrencyRate] = jsonOf

  implicit val encoder: Encoder[CurrencyRate] = deriveEncoder[CurrencyRate]
  implicit def entityEncoder[F[_]]: EntityEncoder[F, CurrencyRate] = jsonEncoderOf

  implicit def seqEncoder[T: Encoder](lst: Seq[T]): Json = lst.asJson
  implicit def seqEntityEncoder[F[_]]: EntityEncoder[F, Seq[CurrencyRate]] = jsonEncoderOf

  implicit val listEncoder: Encoder[List[CurrencyRate]] = deriveEncoder[List[CurrencyRate]]
  implicit def listEntityEncoder[F[_]]: EntityEncoder[F, List[CurrencyRate]] = jsonEncoderOf

  // Doobie Read/Write
  implicit val read: Read[CurrencyRate] =
    Read[(String, Double, String)].map {
      case (curr, rate, actual) => CurrencyRate(
        Currency.withName(curr),
        rate,
        actual.toInstant
      )
    }

  implicit val write: Write[CurrencyRate] =
    Write[(String, Double, String)].contramap(p => (
      p.currency.toString,
      p.rate,
      p.actualAt.toTimeStampString
    ))
}
