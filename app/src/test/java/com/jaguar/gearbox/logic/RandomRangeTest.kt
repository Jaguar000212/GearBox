package com.jaguar.gearbox.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RandomRangeTest {

    @Test
    fun `equal bounds always return that value`() {
        assertEquals(5L, randomLongInclusive(5L, 5L))
    }

    @Test
    fun `result stays within bounds`() {
        repeat(1000) {
            val value = randomLongInclusive(10L, 20L)
            assertTrue(value in 10L..20L)
        }
    }

    @Test
    fun `high equal to Long MAX_VALUE does not throw`() {
        // Regression test: the naive `high + 1` overflows to Long.MIN_VALUE here, which made
        // Random.nextLong(from, until) throw because until <= from.
        repeat(1000) {
            val value = randomLongInclusive(Long.MAX_VALUE - 5, Long.MAX_VALUE)
            assertTrue(value in (Long.MAX_VALUE - 5)..Long.MAX_VALUE)
        }
    }

    @Test
    fun `full long range does not throw`() {
        repeat(1000) {
            randomLongInclusive(Long.MIN_VALUE, Long.MAX_VALUE)
        }
    }
}
