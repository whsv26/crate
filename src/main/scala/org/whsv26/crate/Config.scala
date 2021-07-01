package org.whsv26.crate

object Config {
  final case class PostgresConfig(host: String, port: Int, user: String, password: String, database: String)
  final case class ApiLayerConfig(uri: String)
  final case class AppConfig(db: PostgresConfig, apiLayer: ApiLayerConfig)
}
