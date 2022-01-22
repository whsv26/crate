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
import CurrencyLayerService.{Response, ResponseError}

class CurrencyLayerService[F[_]: Sync](
  client: Client[F],
  conf: AppConfig
) extends CurrencyRateService[F] {

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

    client.expect[Response](GET(query))
      .adaptError { case e => ResponseError(e) }
      .map(adaptResponse)
  }

  private def adaptResponse(rsp: Response) =
    rsp.quotes
      .map { case (currency, rate) => (currency.drop(3), rate) }
      .toList
      .map {
        case (currency, rate) => CurrencyRate(
          Currency.withName(currency),
          rate,
          Instant.ofEpochSecond(rsp.timestamp)
        )
      }
}

object CurrencyLayerService {
  private case class ResponseError(e: Throwable) extends RuntimeException

  private case class Response(
    success: Boolean,
    timestamp: Long,
    source: String,
    quotes: Map[String, Double]
  )

  private object Response {
    implicit def decoder: Decoder[Response] = deriveDecoder[Response]
    implicit def entityDecoder[F[_]: Sync]: EntityDecoder[F, Response] = jsonOf
  }
}


