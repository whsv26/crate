package org.whsv26.crate

import cats.data.NonEmptyList
import cats.effect.Sync
import org.whsv26.crate.Main.TransactorAux

trait CurrencyRateRepository[F[_]] {
  def findByCurrencies(cs: NonEmptyList[Currency]): F[Seq[CurrencyRate]]
  def insertMany(rates: List[CurrencyRate]): F[Int]
}

object CurrencyRateRepository {
  def apply[F[_]: Sync: TransactorAux]: CurrencyRateRepository[F] =
    DoobieCurrencyRateRepository[F]
}


