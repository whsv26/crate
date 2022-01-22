package org.whsv26.crate

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.implicits._
import io.circe._
import io.circe.generic.semiauto._
import org.http4s._
import org.http4s.circe._
import org.http4s.Method.GET
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.whsv26.crate.Config.AppConfig
import java.time.Instant

trait CurrencyLayerService[F[_]] {
  /**
    * Get the most recent exchange rate data
    */
  def getLiveRates(cs: NonEmptyList[Currency]): F[List[CurrencyRate]]
}

object CurrencyLayerService {

  def apply[F[_]](implicit
    ev: CurrencyRateRepository[F]
  ): CurrencyRateRepository[F] = ev

  final case class CurrencyLayerResponseError(e: Throwable) extends RuntimeException

  final case class CurrencyLayerResponse(
    success: Boolean,
    timestamp: Long,
    source: String,
    quotes: Map[String, Double]
  )

  object CurrencyLayerResponse {

    // Circe Encoder/Decoder
    implicit def decoder: Decoder[CurrencyLayerResponse] =
      deriveDecoder[CurrencyLayerResponse]

    type CED[F[_]] = EntityDecoder[F, CurrencyLayerResponse]

    implicit def entityDecoder[F[_] : Sync]: CED[F] = jsonOf
  }

  def impl[F[_] : Sync](
    client: Client[F],
    conf: AppConfig
  ): CurrencyLayerService[F] = new CurrencyLayerService[F] {

      def getLiveRates(cs: NonEmptyList[Currency]): F[List[CurrencyRate]] = {
        val dsl = new Http4sClientDsl[F] { }
        import dsl._

        val uri = Uri.fromString(conf.currencyLayer.uri).toOption.get
        val endpoint = uri / "api" / "live"

        val query = endpoint.withQueryParams(Map(
          "access_key" -> conf.currencyLayer.token,
          "source" -> "USD",
          "format" -> "1",
          "currencies" -> cs.foldLeft("")(_ + "," + _)
        ))

        client.expect[CurrencyLayerResponse](GET(query))
          .adaptError { case e => CurrencyLayerResponseError(e) }
          .map(adaptResponse)
      }

      private def adaptResponse(rsp: CurrencyLayerResponse) =
        rsp.quotes
          .map { case (c, r) => (c.substring(3), r) }
          .toList
          .map {
            case (currency, rate) => CurrencyRate(
              Currency.withName(currency),
              rate,
              Instant.ofEpochSecond(rsp.timestamp)
            )
          }
    }
}


