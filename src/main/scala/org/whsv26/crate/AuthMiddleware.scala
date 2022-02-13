package org.whsv26.crate

import cats._
import cats.data.{Kleisli, OptionT}
import org.http4s.{HttpRoutes, Response}
import org.http4s.Status.Forbidden
import org.http4s.headers.Authorization
import org.whsv26.crate.Config.AppConfig

object AuthMiddleware {
  def apply[F[_]: Applicative](
    route: HttpRoutes[F],
    conf: AppConfig
  ): HttpRoutes[F] = {

    Kleisli { request =>
      val response = for {
        header <- request.headers.get(Authorization)
        token <- header.value.split(' ').lastOption
        _ <- Option.when(token == conf.api.token)(true)
      } yield route(request)

      response.getOrElse(OptionT.pure[F](Response[F](Forbidden)))
    }
  }
}
