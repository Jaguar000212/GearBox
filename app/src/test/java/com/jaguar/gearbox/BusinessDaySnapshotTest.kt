package com.jaguar.gearbox

import com.jaguar.gearbox.data.*
import org.junit.Assert.*
import org.junit.Test

class BusinessDaySnapshotTest {
    private val valid = mapOf("mode" to "ADD", "start" to "2026-11-16", "end" to "2026-11-23",
        "offset" to "5", "weekendMask" to "96", "excludedDates" to "")
    @Test fun snapshotsPreserveAllValidatedTextIncludingDuplicateExclusions() {
        val source = valid + ("excludedDates" to "2026-11-17, 2026-11-18\n2026-11-17")
        assertEquals(source, parseBusinessDaySnapshot(source)!!.toInputs())
        assertNotNull(parseBusinessDaySnapshot(valid + ("mode" to "COUNT")))
        assertNotNull(parseBusinessDaySnapshot(valid + ("offset" to "-10000")))
        assertNotNull(parseBusinessDaySnapshot(valid + ("weekendMask" to "0")))
    }
    @Test fun unsupportedSchemasInvalidDatesAndUnboundedInputsFailClosed() {
        assertNull(parseBusinessDaySnapshot(valid - "end"))
        assertNull(parseBusinessDaySnapshot(valid + ("holidayRegion" to "automatic")))
        for ((key, value) in listOf("mode" to "UNKNOWN", "start" to "1899-12-31", "end" to "2101-01-01",
            "start" to "2025-02-29", "offset" to "+5", "offset" to "05", "offset" to "-0",
            "offset" to "10001", "weekendMask" to "127", "weekendMask" to "096", "excludedDates" to "not a date"))
            assertNull("$key=$value", parseBusinessDaySnapshot(valid + (key to value)))
        assertNull(parseBusinessDaySnapshot(valid + ("excludedDates" to (1..101).joinToString(",") { "2026-11-17" })))
    }
    @Test fun rememberedModeHasNoDateOrExclusionPayload() {
        val route = "tool/business_day_planner"
        assertTrue(ToolMemoryPolicy.validMode(route, mapOf("mode" to "COUNT", "weekendMask" to "96")))
        assertFalse(ToolMemoryPolicy.validMode(route, valid))
        assertFalse(ToolMemoryPolicy.validMode(route, mapOf("mode" to "ADD")))
        assertFalse(ToolMemoryPolicy.validMode(route, mapOf("mode" to "ADD", "weekendMask" to "127")))
        assertFalse(ToolMemorySettings().permitsHistory(route))
        assertFalse(ToolMemorySettings().permitsResult(route))
        assertTrue(ToolMemorySettings(historyEnabled = true).permitsHistory(route))
    }
}
