package com.jaguar.gearbox.logic

import org.junit.Assert.assertEquals
import org.junit.Test

class RatiosTest {

    @Test
    fun `whole numbers simplify via gcd`() {
        assertEquals("2 : 3", simplifyRatio(4.0, 6.0))
        assertEquals("1 : 1", simplifyRatio(5.0, 5.0))
    }

    @Test
    fun `decimal inputs are scaled up before simplifying`() {
        // 1.5 : 2.5 -> 15 : 25 -> 3 : 5
        assertEquals("3 : 5", simplifyRatio(1.5, 2.5))
    }

    @Test
    fun `huge magnitudes do not overflow into a corrupted ratio`() {
        // Regression test: Math.round() silently clamps to Long.MAX_VALUE/MIN_VALUE on overflow
        // instead of throwing, which used to corrupt the simplified ratio for very large inputs.
        val result = simplifyRatio(1.0e17, 2.0e17)
        assertEquals("1 : 2", result)
    }

    @Test
    fun `gcd handles zero`() {
        assertEquals(5L, gcd(5L, 0L))
        assertEquals(5L, gcd(0L, 5L))
    }
}
