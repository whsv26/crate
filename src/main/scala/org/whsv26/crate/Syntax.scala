package org.whsv26.crate

import java.sql.Timestamp
import java.time.Instant

object Syntax {
  implicit class InstantSyntax(val i: Instant) {
    def toTimeStampString: String = Timestamp.from(i).toString
  }
}
