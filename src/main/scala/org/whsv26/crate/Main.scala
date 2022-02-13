package org.whsv26.crate

import cats.effect.{ConcurrentEffect, ExitCode, IO, IOApp, Timer}
import cats.implicits._
import fs2.Stream
import scala.concurrent.duration.DurationInt
import java.util.{Calendar, TimeZone}
import cats.data.Reader

object Main extends IOApp {
  type AppStream[F[_], V] = Reader[AppContext[F], Stream[F, V]]

  def run(args: List[String]): IO[ExitCode] = {
    val reader = for {
      out <- outgoingStream[IO]
      in <- incomingStream[IO]
      merged = out.merge(in).compile.drain.as(ExitCode.Success)
    } yield merged

    def runReader(ctx: AppContext[IO]): IO[ExitCode] = reader.run(ctx)

    AppContext[IO].use(runReader)
  }

  private def outgoingStream[F[_]: ConcurrentEffect: Timer]: AppStream[F, Nothing] =
    Reader { CrateServer.stream[F](_) }

  private def incomingStream[F[_]: ConcurrentEffect: Timer]: AppStream[F, Int] =
    Reader { ctx =>
      Stream
        .awakeEvery[F](1.hour)
        .filter { _ =>
          val now = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
          val dueHours = List(2, 3, 11, 12)
          dueHours.contains(now.get(Calendar.HOUR_OF_DAY))
        }
        .evalMap { _ =>
          val persisted = for {
            rates <- ctx.service.getLiveRates(Currency.nel)
            persistedQty <- ctx.repository.insertMany(rates)
          } yield persistedQty

          persisted.handleError(_ => 0)
        }
        .evalTap(i => { println(i) }.pure[F])
    }
}
