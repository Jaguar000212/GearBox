package com.jaguar.gearbox.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DecimalFractionTest {

    @Test
    fun `terminating decimal`() {
        val fraction = decimalToFraction(0.75)!!
        assertEquals(3L, fraction.numerator)
        assertEquals(4L, fraction.denominator)
        assertEquals("3/4", formatFraction(fraction))
    }

    @Test
    fun `repeating decimal resolves to the simplest fraction`() {
        // The old fixed-9-decimal-digit truncation could fail to recover 1/3 exactly; the
        // continued-fraction approach converges to it from a long run of 3s.
        val fraction = decimalToFraction(0.3333333333)!!
        assertEquals(1L, fraction.numerator)
        assertEquals(3L, fraction.denominator)
    }

    @Test
    fun `whole number has no remainder`() {
        val fraction = decimalToFraction(5.0)!!
        assertEquals("5", formatFraction(fraction))
    }

    @Test
    fun `negative value keeps its sign in the formatted output`() {
        val fraction = decimalToFraction(-1.5)!!
        assertEquals("-3/2\n=  -1 1/2", formatFraction(fraction))
    }

    @Test
    fun `mixed number shown alongside improper fraction`() {
        val fraction = decimalToFraction(1.75)!!
        assertEquals("7/4\n=  1 3/4", formatFraction(fraction))
    }

    @Test
    fun `too large a value returns null instead of throwing`() {
        assertNull(decimalToFraction(1e16))
    }

    @Test
    fun `zero`() {
        val fraction = decimalToFraction(0.0)!!
        assertEquals("0", formatFraction(fraction))
    }
}
