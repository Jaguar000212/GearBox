package com.jaguar.gearbox

import com.jaguar.gearbox.logic.*
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class TimeZoneConversionTest {
    @Test fun springGapIsRejectedNeverShifted() {
        assertEquals(TimeZoneConversionError.GAP, convertTimeZone("2026-03-08", "02:30", "America/New_York", "UTC", "AUTO").error)
    }
    @Test fun overlapRequiresActualOffsetAndProducesTwoInstants() {
        assertEquals(TimeZoneConversionError.OVERLAP_CHOICE_REQUIRED,
            convertTimeZone("2026-11-01", "01:30", "America/New_York", "UTC", "AUTO").error)
        val earlier = convertTimeZone("2026-11-01", "01:30", "America/New_York", "UTC", "-04:00")
        val later = convertTimeZone("2026-11-01", "01:30", "America/New_York", "UTC", "-05:00")
        assertEquals(Instant.parse("2026-11-01T05:30:00Z"), earlier.source!!.toInstant())
        assertEquals(Instant.parse("2026-11-01T06:30:00Z"), later.source!!.toInstant())
        assertEquals(TimeZoneConversionError.INVALID_OFFSET, convertTimeZone("2026-11-01", "01:30", "America/New_York", "UTC", "+01:00").error)
    }
    @Test fun crossMidnightOddOffsetAndSameZone() {
        val result = convertTimeZone("2026-01-15", "23:00", "UTC", "Asia/Kolkata", "AUTO")
        assertEquals("2026-01-16 04:30:00", conversionLocalText(result.target!!))
        assertEquals("+05:30", result.target.offset.id)
        val same = convertTimeZone("2026-01-15", "12:00", "Asia/Kolkata", "Asia/Kolkata", "AUTO")
        assertEquals(same.source, same.target)
    }
    @Test fun strictDatesTimesZonesAndRange() {
        assertNull(parseConversionDate("2025-02-29"))
        assertNotNull(parseConversionDate("2024-02-29"))
        assertNull(parseConversionDate("1899-12-31"))
        assertNull(parseConversionTime("24:00"))
        assertNull(parseConversionTime("1:00"))
        assertFalse(validConversionZone("+05:30"))
        assertEquals(TimeZoneConversionError.INVALID_ZONE, convertTimeZone("2026-01-01", "00:00", "UTC", "Unknown/Nowhere", "AUTO").error)
        assertEquals(TimeZoneConversionError.OUT_OF_RANGE, convertTimeZone("1900-01-01", "00:00", "UTC", "America/New_York", "AUTO").error)
    }
    @Test fun normalTimeDoesNotAcceptStaleOverlapChoice() {
        assertEquals(TimeZoneConversionError.INVALID_OFFSET, convertTimeZone("2026-11-02", "01:30", "America/New_York", "UTC", "-05:00").error)
    }
}
