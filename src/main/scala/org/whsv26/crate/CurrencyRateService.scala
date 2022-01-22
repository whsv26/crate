package org.whsv26.crate

import cats.data.NonEmptyList
import cats.effect.Sync
import org.http4s.client.Client
import org.whsv26.crate.Config.AppConfig

trait CurrencyRateService[F[_]] {
  def getLiveRates(cs: NonEmptyList[Currency]): F[List[CurrencyRate]]
}

object CurrencyRateService {
  def apply[F[_]: Sync](
    client: Client[F],
    conf: AppConfig
  ): CurrencyRateService[F] = {
    new CurrencyLayerService[F](client, conf)
  }
}
