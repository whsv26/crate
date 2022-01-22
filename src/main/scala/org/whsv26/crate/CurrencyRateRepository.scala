package org.whsv26.crate

import cats.data.NonEmptyList
import cats.effect.Sync
import doobie.util.transactor.Transactor

trait CurrencyRateRepository[F[_]] {
  def findByCurrencies(cs: NonEmptyList[Currency]): F[Seq[CurrencyRate]]
  def insertMany(rates: List[CurrencyRate]): F[Int]
}

object CurrencyRateRepository {
  def apply[F[_]: Sync: Transactor]: CurrencyRateRepository[F] =
    DoobieCurrencyRateRepository[F]
}


