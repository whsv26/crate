package org.whsv26.crate

import java.time.{Instant, LocalDateTime, ZoneId, ZoneOffset}

object Syntax {
  implicit class StringSyntax(val s: String) {
    def toInstant: Instant = Instant.from(LocalDateTime.parse(s.replace(' ', 'T')).atZone(ZoneId.of("Z")))
  }
  implicit class InstantSyntax(val i: Instant) {
    def toTimeStampString: String = i.atOffset(ZoneOffset.of("Z")).toLocalDateTime.toString.replace('T', ' ')
  }
}
