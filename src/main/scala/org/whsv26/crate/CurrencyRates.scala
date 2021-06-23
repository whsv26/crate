package org.whsv26.crate

import cats.effect.{ContextShift, IO, LiftIO, Sync}
import cats.implicits._
import io.circe._
import io.circe.generic.semiauto._
import org.http4s._
import org.http4s.circe._
import org.whsv26.crate.Currency.RUB
import doobie._
import doobie.implicits._

import java.time.OffsetDateTime
import scala.concurrent.ExecutionContext


trait CurrencyRates[F[_]]{
  def get: F[CurrencyRates.CurrencyRate]
  def getAll: F[List[CurrencyRates.CurrencyRate]]
}

object CurrencyRates {
  def apply[F[_]](implicit ev: CurrencyRates[F]): CurrencyRates[F] = ev

  final case class CurrencyRate(currency: Currency, rate: Double, actualAt: String)
  object CurrencyRate {
    // Circe
    implicit val currencyRateDecoder: Decoder[CurrencyRate] = deriveDecoder[CurrencyRate]
    implicit def currencyRateEntityDecoder[F[_]: Sync]: EntityDecoder[F, CurrencyRate] = jsonOf

    implicit val currencyRateEncoder: Encoder[CurrencyRate] = deriveEncoder[CurrencyRate]
    implicit def currencyRateEntityEncoder[F[_]]: EntityEncoder[F, CurrencyRate] = jsonEncoderOf

    implicit val currencyRateListEncoder: Encoder[List[CurrencyRate]] = deriveEncoder[List[CurrencyRate]]
    implicit def currencyRateListEntityEncoder[F[_]]: EntityEncoder[F, List[CurrencyRate]] = jsonEncoderOf

    // Doobie
    implicit val currencyRateRead: Read[CurrencyRate] =
      Read[(String, Double, String)].map { case (c, r, aa) => CurrencyRate(Currency.withName(c), r, aa) }
    implicit val currencyRateWrite: Write[CurrencyRate] =
      Write[(String, Double, String)].contramap(p => (p.currency.toString, p.rate, p.actualAt))
  }

  final case class CurrencyRateError(e: Throwable) extends RuntimeException

  def impl[F[_]: Sync]/*(C: Client[F])*/: CurrencyRates[F] = new CurrencyRates[F]{
//    val dsl = new Http4sClientDsl[F]{}
//    import dsl._

    def get: F[CurrencyRate] = CurrencyRate(RUB, 75.01, OffsetDateTime.now().toString).pure[F]
    def getAll: F[List[CurrencyRate]] = {
      implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
      implicit def syncEffectLiftIO: LiftIO[F] =
        new LiftIO[F] {
          override def liftIO[A](ioa: IO[A]): F[A] = {
            ioa.unsafeRunSync().pure[F]
          }
        }


      val xa = Transactor.fromDriverManager[IO](
        "org.postgresql.Driver", "jdbc:postgresql://localhost:54325/crate", "docker", "docker"
      )

      sql"SELECT currency, rate, actual_at FROM currency_rates"
        .query[CurrencyRate]
        .stream
        .compile
        .toList
        .transact(xa)
        .to[F]
    }
  }
}


