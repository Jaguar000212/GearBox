package com.jaguar.gearbox

import com.jaguar.gearbox.data.*
import org.junit.Assert.*
import org.junit.Test

class DeadlineSnapshotTest {
    private val valid = mapOf("deadline" to "2027-01-10", "asOf" to "2027-01-01", "ids" to "1,2",
        "names" to " Prepare \nReview", "durations" to "2,3")
    @Test fun roundTripPreservesOrderNamesAndSavedAsOfDate() {
        val parsed = parseDeadlineSnapshot(valid)!!
        assertEquals(valid, parsed.toInputs())
        assertEquals("2027-01-01", parsed.asOf)
        assertEquals(listOf(1, 2), parsed.phases.map { it.id })
        assertEquals(listOf(2, 3), parsed.phases.map { it.durationDays })
    }
    @Test fun strictShapeIdsDateRangeAndInclusiveStartBoundsFailClosed() {
        assertNull(parseDeadlineSnapshot(valid - "asOf"))
        assertNull(parseDeadlineSnapshot(valid + ("autoToday" to "true")))
        for ((key, value) in listOf("ids" to "1,1", "ids" to "01,2", "names" to "One",
            "durations" to "0,3", "durations" to "3651,3", "durations" to "02,3",
            "deadline" to "1899-12-31", "asOf" to "2101-01-01", "deadline" to "2027-02-29",
            "names" to "A\u2028B\nC", "names" to "A\tB\nC", "names" to "x".repeat(61) + "\nB"))
            assertNull("$key=$value", parseDeadlineSnapshot(valid + (key to value)))
        assertNull(parseDeadlineSnapshot(valid + ("deadline" to "1900-01-01")))
        assertNotNull(parseDeadlineSnapshot(mapOf("deadline" to "1900-01-01", "asOf" to "1900-01-01",
            "ids" to "1", "names" to "One day", "durations" to "1")))
    }
    @Test fun maximumPhaseCountAndTotalAreBoundedWithoutIncreasingStorageLimits() {
        val maximum = valid + mapOf("deadline" to "2100-12-31", "ids" to (1..20).joinToString(","),
            "names" to List(20) { "x".repeat(60) }.joinToString("\n"), "durations" to List(20) { "1825" }.joinToString(","))
        assertNotNull(parseDeadlineSnapshot(maximum))
        assertTrue(ToolMemoryPolicy.validRecord(maximum, "bounded plan"))
        assertNull(parseDeadlineSnapshot(maximum + ("durations" to List(20) { "1826" }.joinToString(","))))
        assertNull(parseDeadlineSnapshot(maximum + mapOf("ids" to (1..21).joinToString(","),
            "names" to List(21) { "A" }.joinToString("\n"), "durations" to List(21) { "1" }.joinToString(","))))
    }
    @Test fun privatePlansRequireSeparateConsentAndCannotEnterModeMemory() {
        val route = "tool/deadline_planner"
        val global = ToolMemorySettings(historyEnabled = true, rememberResults = true)
        assertTrue(ToolMemoryPolicy.isSensitiveTextRoute(route))
        assertFalse(global.permitsHistory(route)); assertFalse(global.permitsResult(route))
        val allowed = global.copy(automaticMemory = mapOf(route to ToolAutomaticMemory(true, true)))
        assertTrue(allowed.permitsHistory(route)); assertTrue(allowed.permitsResult(route))
        assertFalse(ToolMemoryPolicy.validMode(route, valid))
        assertTrue(ToolMemoryPolicy.validMode(route, emptyMap()))
    }
}
