package com.jaguar.gearbox

import com.jaguar.gearbox.logic.calendarDateSnapshot
import com.jaguar.gearbox.logic.daysUntilSummary
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarDateTest {
    private fun clock(instant: String, zone: String) = Clock.fixed(Instant.parse(instant), ZoneId.of(zone))

    @Test fun midnightChangesDateAndSummary() {
        val before = clock("2026-11-14T18:29:59Z", "Asia/Kolkata")
        val after = Clock.offset(before, Duration.ofSeconds(2))
        val target = LocalDate.of(2026, 11, 16)
        assertEquals(1000L, calendarDateSnapshot(before).nextRefreshMillis)
        assertEquals("2 days until trip", daysUntilSummary(calendarDateSnapshot(before).date, target, "trip"))
        assertEquals("1 day until trip", daysUntilSummary(calendarDateSnapshot(after).date, target, "trip"))
    }

    @Test fun resamplingAfterBackgroundOrClockChangeUsesCurrentDate() {
        val before = clock("2026-11-14T12:00:00Z", "UTC")
        val target = LocalDate.of(2026, 11, 16)
        assertEquals("trip is today!", daysUntilSummary(calendarDateSnapshot(Clock.offset(before, Duration.ofDays(2))).date, target, "trip"))
        assertEquals("1 day since trip", daysUntilSummary(calendarDateSnapshot(Clock.offset(before, Duration.ofDays(3))).date, target, "trip"))
        assertEquals("3 days until trip", daysUntilSummary(calendarDateSnapshot(Clock.offset(before, Duration.ofDays(-1))).date, target, "trip"))
    }

    @Test fun timezoneChangeResamplesLocalDate() {
        val west = clock("2026-11-14T18:29:59Z", "America/Los_Angeles")
        assertEquals(LocalDate.of(2026, 11, 14), calendarDateSnapshot(west).date)
        assertEquals(LocalDate.of(2026, 11, 15), calendarDateSnapshot(west.withZone(ZoneId.of("Pacific/Kiritimati"))).date)
    }

    @Test fun midnightSchedulingUsesCalendarBoundaryAcrossDst() {
        assertEquals(23 * 3600000L, calendarDateSnapshot(clock("2026-03-08T05:00:00Z", "America/New_York")).nextRefreshMillis)
        assertEquals(25 * 3600000L, calendarDateSnapshot(clock("2026-11-01T04:00:00Z", "America/New_York")).nextRefreshMillis)
    }
}
