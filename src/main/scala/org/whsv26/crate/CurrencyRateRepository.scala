package org.whsv26.crate

import cats.data.NonEmptyList
import cats.effect.Sync
import doobie._
import doobie.implicits._
import doobie.util.update.Update

trait CurrencyRateRepository[F[_]]{
  def findByCurrencies(cs: NonEmptyList[Currency]): F[List[CurrencyRate]]
  def insertMany(rates: List[CurrencyRate]): F[Int]
}

object CurrencyRateRepository {
  def apply[F[_]](implicit ev: CurrencyRateRepository[F]): CurrencyRateRepository[F] = ev

  def impl[F[_]: Sync](xa: Transactor.Aux[F, Unit]): CurrencyRateRepository[F] = new CurrencyRateRepository[F]{
    def findByCurrencies(cs: NonEmptyList[Currency]): F[List[CurrencyRate]] = {
      val q = fr"""
          SELECT currency, rate, actual_at
          FROM currency_rates
          WHERE""" ++ Fragments.in(fr"currency", cs.map(_.toString))

      q.query[CurrencyRate]
        .stream
        .compile
        .toList
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


