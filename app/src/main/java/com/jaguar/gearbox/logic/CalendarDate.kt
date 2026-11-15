package com.jaguar.gearbox.logic

import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal data class CalendarDateSnapshot(val date: LocalDate, val nextRefreshMillis: Long)

/** Read instant and zone once so the date and its next boundary agree, including DST. */
internal fun calendarDateSnapshot(clock: Clock): CalendarDateSnapshot {
    val now = clock.instant()
    val date = now.atZone(clock.zone).toLocalDate()
    val next = date.plusDays(1).atStartOfDay(clock.zone).toInstant()
    return CalendarDateSnapshot(date, Duration.between(now, next).toMillis().coerceAtLeast(1))
}

internal fun daysUntilSummary(today: LocalDate, target: LocalDate, label: String): String {
    val days = ChronoUnit.DAYS.between(today, target)
    val event = label.ifBlank { "the date" }
    return when {
        days > 0 -> "$days day${if (days == 1L) "" else "s"} until $event"
        days == 0L -> "$event is today!"
        else -> "${-days} day${if (days == -1L) "" else "s"} since $event"
    }
}
