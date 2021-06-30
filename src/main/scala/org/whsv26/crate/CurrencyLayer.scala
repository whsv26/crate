package org.whsv26.crate

import cats.effect.Sync
import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.util.update.Update
import io.circe._
import io.circe.generic.semiauto._
import org.http4s._
import org.http4s.implicits._
import org.http4s.circe._
import org.http4s.Method.GET
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.whsv26.crate.CurrencyRates.CurrencyRate
import java.time.Instant

trait CurrencyLayer[F[_]] {
  def getCurrentRates(cs: List[Currency]): F[List[CurrencyRate]]

  def persistCurrencyRates(rates: List[CurrencyRate]): F[Int]
}

object CurrencyLayer {
  def apply[F[_]](implicit ev: CurrencyRates[F]): CurrencyRates[F] = ev

  final case class CurrencyLayerResponseError(e: Throwable) extends RuntimeException

  final case class CurrencyLayerResponse(success: Boolean,
                                         timestamp: Long,
                                         source: String,
                                         quotes: Map[String, Double])

  object CurrencyLayerResponse {
    // Circe Encoder/Decoder
    implicit val currencyLayerResponseDecoder: Decoder[CurrencyLayerResponse] = deriveDecoder[CurrencyLayerResponse]

    implicit def currencyLayerResponseEntityDecoder[F[_] : Sync]: EntityDecoder[F, CurrencyLayerResponse] = jsonOf
  }

  def impl[F[_] : Sync](C: Client[F], xa: Transactor.Aux[F, Unit]): CurrencyLayer[F] = new CurrencyLayer[F] {
    def getCurrentRates(cs: List[Currency]): F[List[CurrencyRate]] = {
      val dsl = new Http4sClientDsl[F] {}
      import dsl._

      // TODO Drop mock server
      val baseUri = uri"https://14c373a2-f86b-49a6-ae99-fe4a4dcecc23.mock.pstmn.io" / "api" / "live"
      val uri = baseUri.withQueryParams(Map(
        "access_key" -> "",
        "source" -> "USD",
        "format" -> "1",
        "currencies" -> cs.foldLeft("")(_ + "," + _)
      ))

      C.expect[CurrencyLayerResponse](GET(uri))
        .adaptError { case e => CurrencyLayerResponseError(e) }
        .map(rsp => {
          val quotes = rsp.quotes map { case (c, r) => (c.replace(rsp.source, ""), r) }
          quotes.toList map {
            case (currency, rate) => CurrencyRate(
              Currency.withName(currency),
              rate,
              Instant.ofEpochSecond(rsp.timestamp)
            )
          }
        })
    }

    def persistCurrencyRates(rates: List[CurrencyRate]): F[Int] = {
      val sql = "INSERT INTO currency_rates (currency, rate, actual_at) values (?, ?, ?::TIMESTAMP)"
      Update[CurrencyRate](sql)
        .updateMany(rates)
        .transact(xa)
    }
  }
}


