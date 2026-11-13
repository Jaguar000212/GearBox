package com.jaguar.gearbox.logic

import org.junit.Assert.*
import org.junit.Test

class CounterTest {
    @Test fun `steps preserve ordinary and negative counts`() {
        assertEquals(15, nextCounterValue(5, 10))
        assertEquals(-5, nextCounterValue(5, -10))
    }

    @Test fun `overflow is rejected instead of wrapping`() {
        assertNull(nextCounterValue(Int.MAX_VALUE, 1))
        assertNull(nextCounterValue(Int.MIN_VALUE, -1))
        assertEquals(Int.MAX_VALUE, nextCounterValue(Int.MAX_VALUE - 10, 10))
        assertEquals(Int.MIN_VALUE, nextCounterValue(Int.MIN_VALUE + 5, -5))
    }
}
