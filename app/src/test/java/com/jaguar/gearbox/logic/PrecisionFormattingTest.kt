package com.jaguar.gearbox.logic

import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class PrecisionFormattingTest {
    @Test fun tinyNonzeroValuesKeepSignAndMagnitude() {
        assertEquals("1e-7", formatNonzeroTrimmed(1e-7))
        assertEquals("-1e-7", formatNonzeroTrimmed(-1e-7))
        assertEquals("4.9e-7", formatNonzeroTrimmed(4.9e-7))
        assertEquals("1.234568e-9", formatNonzeroTrimmed(1.23456789e-9))
        assertNotEquals("0", formatNonzeroTrimmed(Double.MIN_VALUE))
    }

    @Test fun normalOutputAndRoundingBoundaryStayUnchanged() {
        listOf(0.0, -0.0, 5e-7, -5e-7, 1e-6, 0.1, 12.5, 1.23456789, 1e20,
            Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).forEach {
            assertEquals(formatTrimmed(it), formatNonzeroTrimmed(it))
        }
        assertEquals("0", formatNonzeroTrimmed(-0.0))
        assertEquals("0.000001", formatNonzeroTrimmed(5e-7))
    }

    @Test fun fixedDecimalFinancialFormattingIsUnchanged() {
        assertEquals("0", formatTrimmed(0.001, 2))
        assertEquals("12.35", formatTrimmed(12.345, 2))
        assertEquals("0", formatTrimmed(1e-7))
    }

    @Test fun fallbackDoesNotDependOnDeviceLocale() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals("1.25e-7", formatNonzeroTrimmed(1.25e-7))
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test fun millimetresToKilometresRemainNonzero() {
        val result = 0.1 * 0.001 / 1000.0
        assertEquals("1e-7", formatNonzeroTrimmed(result))
        assertEquals("0.1 mm = 1e-7 km",
            "${formatNonzeroTrimmed(0.1)} mm = ${formatNonzeroTrimmed(result)} km")
    }

    @Test fun tinyAverageStatisticsAndExportAgree() {
        val result = computeAverageStatistics("0.0000001")!!
        listOf(result.arithmetic, result.geometric, result.harmonic,
            result.sum, result.smallest, result.largest).forEach { assertEquals("1e-7", it) }
        assertEquals(1, result.count)
        assertEquals("Result: 1e-7\n\nGeometric Mean: 1e-7\nHarmonic Mean: 1e-7\n\n" +
            "Sum: 1e-7\nCount: 1\nLargest: 1e-7\nSmallest: 1e-7", result.asText())
        assertEquals(result.asText(), computeAverageResult("0.0000001"))
    }

    @Test fun negativeAndZeroAveragesRetainSemantics() {
        val negative = computeAverageStatistics("-0.0000001")!!
        assertEquals("-1e-7", negative.arithmetic)
        assertEquals("-1e-7", negative.harmonic)
        assertTrue(negative.geometric.contains("negative"))
        val zero = computeAverageStatistics("0")!!
        assertEquals("0", zero.arithmetic)
        assertEquals("0", zero.geometric)
        assertTrue(zero.harmonic.contains("zero"))
    }
}
