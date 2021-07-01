package org.whsv26.crate

import java.sql.Timestamp
import java.time.Instant

object Syntax {
  implicit class StringSyntax(val s: String) {
    def toInstant: Instant = Timestamp.valueOf(s).toInstant
  }
  implicit class InstantSyntax(val i: Instant) {
    def toTimeStampString: String = Timestamp.from(i).toString
  }
}
