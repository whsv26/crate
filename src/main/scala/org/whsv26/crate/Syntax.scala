package org.whsv26.crate

import java.sql.Timestamp
import java.time.{Instant, LocalDateTime, ZoneId}

object Syntax {
  implicit class StringSyntax(val s: String) {
    def toInstant: Instant = Instant.from(LocalDateTime.parse(s.replace(' ', 'T')).atZone(ZoneId.of("Z")))
  }
  implicit class InstantSyntax(val i: Instant) {
    def toTimeStampString: String = Timestamp.from(i).toString
  }
}
