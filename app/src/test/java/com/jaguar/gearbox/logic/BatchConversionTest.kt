package com.jaguar.gearbox.logic

import org.junit.Assert.*
import org.junit.Test

class BatchConversionTest {
    @Test fun rowsIncludeBlankInvalidAndOverflowWithoutSkipping() {
        val result = convertNumericBatch("1\r\n\r\nbad\nNaN\n1e308\n-2") { it * 1000 }
        assertNull(result.error)
        assertEquals(6, result.rows.size)
        assertEquals((1..6).toList(), result.rows.map { it.line })
        assertEquals(1000.0, result.rows[0].result!!, 0.0)
        assertEquals(BatchRowError.INVALID_NUMBER, result.rows[1].error)
        assertEquals(BatchRowError.INVALID_NUMBER, result.rows[2].error)
        assertEquals(BatchRowError.INVALID_NUMBER, result.rows[3].error)
        assertEquals(BatchRowError.OUT_OF_RANGE, result.rows[4].error)
        assertEquals(-2000.0, result.rows[5].result!!, 0.0)
    }
    @Test fun limitsRejectWholeSubmissionNeverTruncate() {
        assertEquals(BatchInputError.TOO_LONG, convertNumericBatch("1".repeat(4097)) { error("Must not run") }.error)
        val tooMany = convertNumericBatch(List(101) { "1" }.joinToString("\n")) { error("Must not run") }
        assertEquals(BatchInputError.TOO_MANY_LINES, tooMany.error)
        assertTrue(tooMany.rows.isEmpty())
        assertEquals(100, convertNumericBatch(List(100) { "1" }.joinToString("\n")) { it }.rows.size)
        assertEquals(2, convertNumericBatch("1\n") { it }.rows.size)
    }
    @Test fun temperatureAndReverseUseFiniteRoundTripNotRoundedDisplay() {
        assertEquals(0.0, convertNumericBatch("32") { (it - 32) * 5 / 9 }.rows.single().result!!, 0.0)
        val converted = 1.0 / 3
        assertEquals(converted, reverseConversionInput(converted)!!.toDouble(), 0.0)
        assertNull(reverseConversionInput(Double.NaN))
        assertNull(reverseConversionInput(Double.POSITIVE_INFINITY))
    }
}
