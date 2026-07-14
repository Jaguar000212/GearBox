package com.jaguar.gearbox.logic

import org.junit.Assert.assertEquals
import org.junit.Test

class GstTest {

    @Test
    fun `exclusive tax adds on top of the base amount`() {
        val result = calculateGst(100.0, 18.0, inclusive = false)
        assertEquals(100.0, result.baseAmount, 0.001)
        assertEquals(18.0, result.taxAmount, 0.001)
        assertEquals(118.0, result.totalAmount, 0.001)
    }

    @Test
    fun `inclusive tax backs out the base from a tax-included amount`() {
        val result = calculateGst(118.0, 18.0, inclusive = true)
        assertEquals(100.0, result.baseAmount, 0.001)
        assertEquals(18.0, result.taxAmount, 0.001)
        assertEquals(118.0, result.totalAmount, 0.001)
    }

    @Test
    fun `zero rate leaves amount unchanged either direction`() {
        assertEquals(100.0, calculateGst(100.0, 0.0, inclusive = false).totalAmount, 0.001)
        assertEquals(100.0, calculateGst(100.0, 0.0, inclusive = true).totalAmount, 0.001)
    }
}
