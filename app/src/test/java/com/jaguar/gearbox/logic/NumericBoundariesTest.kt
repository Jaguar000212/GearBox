package com.jaguar.gearbox.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NumericBoundariesTest {
    @Test
    fun `ratios preserve tiny and beyond Long magnitudes`() {
        assertEquals("1 : 2", simplifyRatio(1e-12, 2e-12))
        assertEquals("1 : 2", simplifyRatio(1e100, 2e100))
        assertEquals("-1 : 2", simplifyRatio(-1e100, 2e100))
        assertEquals("0 : 1", simplifyRatio(0.0, 1e-12))
        assertNull(simplifyRatio(0.0, 0.0))
        assertNull(simplifyRatio(Double.NaN, 1.0))
        assertNull(simplifyRatio(1.0, Double.POSITIVE_INFINITY))
    }

    @Test
    fun `bmi rejects nonfinite or unrepresentable calculations`() {
        assertNull(computeBmi(Double.NaN, 70.0))
        assertNull(computeBmi(0.0, 70.0))
        assertNull(computeBmi(1e-300, 70.0))
        assertNull(computeBmi(1e300, 70.0))
        assertNull(computeBmi(1.75, Double.POSITIVE_INFINITY))
        assertNotNull(computeBmi(1.75, 70.0))
    }

    @Test
    fun `tax rejects overflow but avoids unnecessary intermediate overflow`() {
        assertNull(calculateGst(Double.MAX_VALUE, 100.0, false))
        assertNull(calculateGst(10.0, Double.NaN, false))
        assertNull(calculateGst(-10.0, 18.0, false))
        val large = calculateGst(1e308, 1.0, false)!!
        assertTrue(large.totalAmount.isFinite())
        assertEquals(1e306, large.taxAmount, 1e292)
    }

    @Test
    fun `fuel rejects invalid inputs and overflowing results`() {
        assertNull(calculateFuelCost(300.0, Double.NaN, 100.0))
        assertNull(calculateFuelCost(-1.0, 10.0, 1.0))
        assertNull(calculateFuelCost(1.0, 10.0, -1.0))
        assertNull(calculateFuelCost(Double.MAX_VALUE, Double.MIN_VALUE, 1.0))
        assertNull(calculateFuelCost(Double.MAX_VALUE, 1.0, 2.0))
        assertNull(calculateFuelCost(Double.MIN_VALUE, 2.0, 1.0))
        assertEquals(0.0, calculateFuelCost(0.0, 10.0, 100.0)!!.totalCost, 0.0)
    }

    @Test
    fun `fraction rejects nonfinite and does not turn tiny nonzero input into zero`() {
        assertNull(decimalToFraction(Double.NaN))
        assertNull(decimalToFraction(Double.POSITIVE_INFINITY))
        assertNull(decimalToFraction(1e-12))
        assertEquals(Fraction(1, 1_000_000), decimalToFraction(1e-6))
        assertEquals("-9223372036854775808", formatFraction(Fraction(Long.MIN_VALUE, 1)))
    }

    @Test
    fun `formatting zero decimals preserves integer trailing zeros`() {
        assertEquals("100", formatTrimmed(100.0, 0))
        assertEquals("0", formatTrimmed(-0.0, 0))
        assertEquals("12.5", formatTrimmed(12.5, 4))
    }

    @Test
    fun `average rejects nonfinite input and overflow`() {
        assertNull(computeAverageResult("NaN, 1"))
        assertNull(computeAverageResult("Infinity, 1"))
        assertNull(computeAverageResult("1e308, 1e308"))
        val result = computeAverageResult("1e200, 1e200")!!
        assertFalse(result.contains("Infinity"))
        assertFalse(result.contains("NaN"))
        assertTrue(computeAverageResult("1,-1")!!.contains("N/A (undefined or outside numeric range)"))
    }

    @Test
    fun `loan supports zero and tiny interest while rejecting overflow`() {
        assertTrue(computeLoanResult(1200.0, 0.0, 1.0)!!.contains("Monthly Installment: 100.00"))
        val tinyRate = computeLoanResult(1200.0, 1e-12, 1.0)!!
        assertFalse(tinyRate.contains("NaN"))
        assertFalse(tinyRate.contains("Infinity"))
        assertNull(computeLoanResult(1200.0, 10.0, Double.MAX_VALUE))
        assertNull(computeLoanResult(Double.MAX_VALUE, 100.0, 10.0))
        assertNull(computeLoanResult(1200.0, Double.NaN, 1.0))
        assertNull(computeLoanResult(Double.MIN_VALUE, 0.0, 1.0))
    }
}
