package org.whsv26.crate

object Config {
  final case class PostgresConfig(host: String, port: Int, user: String, password: String, database: String)
  final case class CurrencyLayerConfig(uri: String, token: String)
  final case class AppConfig(db: PostgresConfig, currencyLayer: CurrencyLayerConfig)
}
