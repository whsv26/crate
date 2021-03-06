package org.whsv26.crate

object Config {
  final case class ApiConfig(token: String)
  final case class ServerConfig(host: String, port: Int)
  final case class CurrencyLayerConfig(uri: String, token: String)

  final case class PostgresConfig(
    host: String,
    port: Int,
    user: String,
    password: String,
    database: String
  )

  final case class AppConfig(
    api: ApiConfig,
    db: PostgresConfig,
    currencyLayer: CurrencyLayerConfig,
    server: ServerConfig
  )
}
