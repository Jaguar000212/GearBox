package com.jaguar.gearbox

import com.jaguar.gearbox.logic.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import org.junit.Assert.*
import org.junit.Test

class DeadlinePlannerTest {
    private fun date(text: String) = LocalDate.parse(text)
    @Test fun actualBackwardInclusiveSchedule() {
        val result = planBackwardDeadline(date("2027-01-10"), date("2027-01-01"), listOf(DeadlinePhase(1,"Prepare",2), DeadlinePhase(2,"Review",3)))
        assertEquals(date("2027-01-06"), result.earliestStart)
        assertEquals(date("2027-01-07"), result.phases[0].end)
        assertEquals(date("2027-01-08"), result.phases[1].start)
        assertEquals(date("2027-01-10"), result.phases[1].end)
        assertEquals(5, result.totalDays)
        result.phases.forEach { assertEquals(it.durationDays.toLong(), ChronoUnit.DAYS.between(it.start, it.end) + 1) }
    }
    @Test fun oneDayLeapAndSavedAsOfSemantics() {
        val result = planBackwardDeadline(date("2024-03-01"), date("2024-03-02"), listOf(DeadlinePhase(1,"First",1), DeadlinePhase(2,"Last",1)))
        assertEquals(date("2024-02-29"), result.phases[0].start)
        assertEquals(result.phases[0].start, result.phases[0].end)
        assertEquals(-2L, result.phases[0].daysUntilFinish)
        assertEquals(-1L, result.phases[1].daysUntilFinish)
    }
    @Test fun reorderedIdsKeepDurationsAndChangeTheirDates() {
        val a = DeadlinePhase(11,"A",2); val b = DeadlinePhase(22,"B",3)
        val original = planBackwardDeadline(date("2027-01-10"), date("2027-01-10"), listOf(a,b))
        val reordered = planBackwardDeadline(date("2027-01-10"), date("2027-01-10"), listOf(b,a))
        assertEquals(original.earliestStart, reordered.earliestStart)
        assertEquals(22, reordered.phases[0].id)
        assertEquals(3, reordered.phases[0].durationDays)
        assertEquals(date("2027-01-08"), reordered.phases[0].end)
        assertEquals(0L, reordered.phases.last().daysUntilFinish)
    }
    @Test fun boundsAndInvalidPhaseDataReject() {
        assertEquals(DeadlinePlanError.OUT_OF_RANGE, planBackwardDeadline(date("1900-01-01"), date("1900-01-01"), listOf(DeadlinePhase(1,"A",2))).error)
        assertNull(planBackwardDeadline(date("1900-01-01"), date("1900-01-01"), listOf(DeadlinePhase(1,"A",1))).error)
        assertEquals(DeadlinePlanError.INVALID_INPUT, planBackwardDeadline(date("2100-12-31"), date("2100-01-01"), List(11) { DeadlinePhase(it+1,"A",3650) }).error)
        assertEquals(DeadlinePlanError.INVALID_INPUT, planBackwardDeadline(date("2027-01-10"), date("2027-01-01"), listOf(DeadlinePhase(1,"A",0))).error)
        assertFalse(validDeadlineLabel("a\u2028b")); assertNull(parseDeadlineDate("2025-02-29"))
    }
}
