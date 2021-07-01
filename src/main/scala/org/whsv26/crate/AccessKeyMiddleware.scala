package org.whsv26.crate

import cats._
import cats.data.{Kleisli, OptionT}
import org.http4s.{HttpRoutes, Response}
import org.http4s.Status.Forbidden
import org.whsv26.crate.Config.AppConfig

object AccessKeyMiddleware {
  def apply[F[_]: Applicative](route: HttpRoutes[F], conf: AppConfig): HttpRoutes[F] = Kleisli { request =>
    val matchesOpt = for {
      accessKey <- request.params.get("access_key")
      matches <- Option.when(accessKey == conf.api.accessKey)(true)
    } yield matches
    val isValidToken = matchesOpt.getOrElse(false)

    if (isValidToken) {
      route(request)
    } else {
      OptionT.pure[F](Response[F](Forbidden))
    }
  }
}
