package org.whsv26.crate

import cats.data.NonEmptyList
import doobie.util.update.Update
import cats.effect.Sync
import cats.implicits.toFoldableOps
import doobie.implicits._
import org.whsv26.crate.DoobieCurrencyRateRepository.{findByCurrenciesFragment, insertManyFragment}
import org.whsv26.crate.Main.TransactorAux

class DoobieCurrencyRateRepository[F[_]: Sync](implicit
  xa: TransactorAux[F]
) extends CurrencyRateRepository[F] {

  def findByCurrencies(cs: NonEmptyList[Currency]): F[Seq[CurrencyRate]] =
    findByCurrenciesFragment(cs)
      .query[CurrencyRate]
      .stream
      .compile
      .to(Seq)
      .transact(xa)

  def insertMany(rates: List[CurrencyRate]): F[Int] =
    Update[CurrencyRate](insertManyFragment.toString())
      .updateMany(rates)
      .transact(xa)
}

object DoobieCurrencyRateRepository {
  def apply[F[_]: Sync: TransactorAux]: DoobieCurrencyRateRepository[F] = {
    new DoobieCurrencyRateRepository
  }

  private def findByCurrenciesFragment(cs: NonEmptyList[Currency]) = {
    val valuesFr = cs.map(_.toString)
      .map(n => fr"($n)")
      .intercalate(fr",")

    fr"WITH currencies(currency) AS (VALUES " ++ valuesFr ++ fr")" ++
    fr"""
      |SELECT c.currency, r.rate, r.actual_at
      |FROM currencies c
      |INNER JOIN LATERAL (
      |  SELECT currency, rate, actual_at
      |  FROM currency_rates cr
      |  WHERE cr.currency = c.currency
      |  ORDER BY cr.actual_at DESC
      |  LIMIT 1
      |) r ON TRUE
      |""".stripMargin('|')
  }

  private val insertManyFragment = fr"""
    |INSERT INTO currency_rates (
    |  currency,
    |  rate,
    |  actual_at
    |) VALUES (
    |  ?,
    |  ?,
    |  ?::TIMESTAMP
    |)
    |""".stripMargin('|')
}