package org.whsv26.crate

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.implicits.toFoldableOps
import doobie._
import doobie.implicits._
import doobie.util.update.Update

trait CurrencyRateRepository[F[_]]{
  def findByCurrencies(cs: NonEmptyList[Currency]): F[Seq[CurrencyRate]]
  def insertMany(rates: List[CurrencyRate]): F[Int]
}

object CurrencyRateRepository {

  def apply[F[_]](implicit
    repo: CurrencyRateRepository[F]
  ): CurrencyRateRepository[F] = repo

  def impl[F[_]: Sync](xa: Transactor.Aux[F, Unit]): CurrencyRateRepository[F] =
    new CurrencyRateRepository[F] {
      def findByCurrencies(cs: NonEmptyList[Currency]): F[Seq[CurrencyRate]] = {
        val currValuesFr = cs.map(_.toString).map(n => fr"($n)").intercalate(fr",")
        val cteFr = fr"WITH currencies(currency) AS (VALUES " ++ currValuesFr ++ fr")"
        val queryFr = cteFr ++ fr"""
            SELECT c.currency, r.rate, r.actual_at
            FROM currencies c
            INNER JOIN LATERAL (
              SELECT currency, rate, actual_at
              FROM currency_rates cr
              WHERE cr.currency = c.currency
              ORDER BY cr.actual_at DESC
              LIMIT 1
            ) r ON TRUE
        """

        queryFr.query[CurrencyRate]
          .stream
          .compile
          .to(Seq)
          .transact(xa)
      }

      def insertMany(rates: List[CurrencyRate]): F[Int] = {
        val sql = "INSERT INTO currency_rates (currency, rate, actual_at) values (?, ?, ?::TIMESTAMP)"

        Update[CurrencyRate](sql)
          .updateMany(rates)
          .transact(xa)
      }
    }
}


