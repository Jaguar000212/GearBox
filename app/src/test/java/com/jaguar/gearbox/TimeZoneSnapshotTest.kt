package com.jaguar.gearbox

import com.jaguar.gearbox.data.*
import com.jaguar.gearbox.logic.*
import org.junit.Assert.*
import org.junit.Test

class TimeZoneSnapshotTest {
    private val valid = mapOf("date" to "2026-11-17", "time" to "12:00", "sourceZone" to "UTC",
        "targetZone" to "Asia/Kolkata", "sourceOffset" to "AUTO")
    @Test fun rawSnapshotRoundTripsWithoutOffsetNormalization() {
        assertEquals(valid, parseTimeZoneSnapshot(valid)!!.toInputs())
        assertNotNull(parseTimeZoneSnapshot(valid + ("date" to "1900-01-01")))
        assertNotNull(parseTimeZoneSnapshot(valid + ("date" to "2100-12-31")))
    }
    @Test fun gapRejectedAndOverlapRemainsAnExplicitChoice() {
        val ny = valid + mapOf("sourceZone" to "America/New_York", "date" to "2026-11-01", "time" to "01:30")
        assertNotNull(parseTimeZoneSnapshot(ny))
        assertEquals(TimeZoneConversionError.OVERLAP_CHOICE_REQUIRED,
            convertTimeZone(ny.getValue("date"), ny.getValue("time"), ny.getValue("sourceZone"), ny.getValue("targetZone"), "AUTO").error)
        for (offset in listOf("-04:00", "-05:00")) assertEquals(ny + ("sourceOffset" to offset),
            parseTimeZoneSnapshot(ny + ("sourceOffset" to offset))!!.toInputs())
        assertNull(parseTimeZoneSnapshot(ny + ("sourceOffset" to "-06:00")))
        assertNull(parseTimeZoneSnapshot(ny + mapOf("date" to "2026-03-08", "time" to "02:30")))
    }
    @Test fun malformedUnknownAndExtraEventFieldsFailClosed() {
        assertNull(parseTimeZoneSnapshot(valid - "date"))
        assertNull(parseTimeZoneSnapshot(valid + ("location" to "private")))
        for ((key, value) in listOf("date" to "1899-12-31", "date" to "2101-01-01", "date" to "2026-02-30",
            "time" to "24:00", "time" to "1:30", "sourceZone" to "Bogus/Zone", "targetZone" to "+05:00",
            "sourceOffset" to "Z")) assertNull(parseTimeZoneSnapshot(valid + (key to value)))
    }
    @Test fun modesRememberOnlyKnownZonesNotSpecificDatesOrOverlapChoice() {
        val route = "tool/time_zone_converter"
        assertTrue(ToolMemoryPolicy.validMode(route, mapOf("sourceZone" to "UTC", "targetZone" to "Asia/Kolkata")))
        assertFalse(ToolMemoryPolicy.validMode(route, valid))
        assertFalse(ToolMemoryPolicy.validMode(route, mapOf("sourceZone" to "UTC", "targetZone" to "unknown")))
        assertFalse(ToolMemorySettings().permitsHistory(route))
        assertFalse(ToolMemorySettings().permitsResult(route))
    }
}
