package org.whsv26.crate

object Config {
  final case class PostgresConfig(host: String, port: Int, user: String, password: String, database: String)
}
