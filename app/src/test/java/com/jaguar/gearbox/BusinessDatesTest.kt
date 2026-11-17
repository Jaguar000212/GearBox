package com.jaguar.gearbox

import com.jaguar.gearbox.logic.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class BusinessDatesTest {
    private fun date(text: String) = LocalDate.parse(text)
    @Test fun offsetsSkipWeekendsExclusionsAndHandleZero() {
        val friday = date("2026-11-20")
        assertEquals(date("2026-11-23"), businessDateOffset(friday, 1, 96, emptySet()))
        assertEquals(date("2026-11-24"), businessDateOffset(friday, 1, 96, setOf(date("2026-11-23"))))
        assertEquals(friday, businessDateOffset(date("2026-11-23"), -1, 96, emptySet()))
        val saturday = date("2026-11-21")
        assertEquals(saturday, businessDateOffset(saturday, 0, 96, emptySet()))
    }
    @Test fun directionalEndpointRuleIsExplicit() {
        val sunday = date("2026-11-22"); val monday = date("2026-11-23")
        assertEquals(1, countBusinessDates(sunday, monday, 96, emptySet()))
        assertEquals(0, countBusinessDates(monday, sunday, 96, emptySet()))
        assertEquals(-1, countBusinessDates(monday, date("2026-11-20"), 96, emptySet()))
        assertEquals(0, countBusinessDates(monday, monday, 96, emptySet()))
    }
    @Test fun leapDayAndNoWeekendAreCalendarOnly() {
        assertEquals(date("2024-02-29"), businessDateOffset(date("2024-02-28"), 1, 0, emptySet()))
        assertEquals(2, countBusinessDates(date("2024-02-28"), date("2024-03-01"), 0, emptySet()))
        assertNull(parseBusinessDate("2023-02-29"))
        assertNull(parseBusinessDate("2101-01-01"))
    }
    @Test fun exclusionsBoundAndDeduplicateWithoutDoubleSubtractingWeekends() {
        assertEquals(setOf(date("2026-11-21")), parseBusinessExclusions("2026-11-21, 2026-11-21"))
        assertEquals(1, countBusinessDates(date("2026-11-20"), date("2026-11-23"), 96, setOf(date("2026-11-21"))))
        assertNull(parseBusinessExclusions(List(101) { "2026-11-21" }.joinToString(",")))
        assertNull(parseBusinessExclusions("2026-11-21,"))
        assertNull(parseBusinessWeekendMask("127"))
        assertNull(parseBusinessOffset("-0"))
        assertNull(parseBusinessOffset("10001"))
    }
    @Test(expected = IllegalArgumentException::class) fun rangeOverflowFailsInsteadOfClamping() {
        businessDateOffset(BUSINESS_DATE_MAX, 1, 0, emptySet())
    }
    @Test(expected = IllegalArgumentException::class) fun allWeekdaysExcludedRejected() {
        businessDateOffset(date("2026-11-20"), 1, 127, emptySet())
    }
}
