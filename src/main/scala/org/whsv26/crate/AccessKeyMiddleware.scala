package org.whsv26.crate

import cats._
import cats.data.{Kleisli, OptionT}
import org.http4s.{HttpRoutes, Response}
import org.http4s.Status.Forbidden
import org.whsv26.crate.Config.AppConfig

object AccessKeyMiddleware {
  def apply[F[_]: Applicative](
    route: HttpRoutes[F],
    conf: AppConfig
  ): HttpRoutes[F] = {

    Kleisli { request =>
      val response = for {
        accessKey <- request.params.get("access_key")
        _ <- Option.when(accessKey == conf.api.accessKey)(true)
      } yield route(request)

      response.getOrElse(OptionT.pure[F](Response[F](Forbidden)))
    }
  }
}
