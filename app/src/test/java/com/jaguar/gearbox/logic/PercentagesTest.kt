package com.jaguar.gearbox.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PercentagesTest {
    @Test fun familiarExamples() {
        assertEquals(36.0, calculatePercentage(PercentageOperation.OF_VALUE, 15.0, 240.0)!!, 0.0001)
        assertEquals(20.0, calculatePercentage(PercentageOperation.WHAT_PERCENT, 30.0, 150.0)!!, 0.0001)
        assertEquals(25.0, calculatePercentage(PercentageOperation.CHANGE, 80.0, 100.0)!!, 0.0001)
        assertEquals(-20.0, calculatePercentage(PercentageOperation.CHANGE, 100.0, 80.0)!!, 0.0001)
    }
    @Test fun rejectsUndefinedAndNonFiniteResults() {
        assertNull(calculatePercentage(PercentageOperation.WHAT_PERCENT, 1.0, 0.0))
        assertNull(calculatePercentage(PercentageOperation.CHANGE, 0.0, 10.0))
        assertNull(calculatePercentage(PercentageOperation.OF_VALUE, Double.NaN, 10.0))
        assertNull(calculatePercentage(PercentageOperation.OF_VALUE, Double.MAX_VALUE, Double.MAX_VALUE))
    }
    @Test fun preservesZeroAndSignedBaseSemantics() {
        assertEquals(0.0, calculatePercentage(PercentageOperation.OF_VALUE, 0.0, 50.0)!!, 0.0)
        assertEquals(-50.0, calculatePercentage(PercentageOperation.CHANGE, -100.0, -50.0)!!, 0.0)
        assertEquals(0.0, calculatePercentage(PercentageOperation.CHANGE, 20.0, 20.0)!!, 0.0)
    }
}
