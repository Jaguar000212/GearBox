package com.jaguar.gearbox.logic

import org.junit.Assert.assertEquals
import org.junit.Test

class DiscountTest {

    @Test
    fun `single discount matches simple percentage math`() {
        val result = applyStackedDiscounts(100.0, listOf(20.0))
        assertEquals(80.0, result.finalPrice, 0.001)
        assertEquals(20.0, result.totalSaved, 0.001)
        assertEquals(20.0, result.effectivePercent, 0.001)
    }

    @Test
    fun `stacked discounts compound rather than sum`() {
        val result = applyStackedDiscounts(100.0, listOf(20.0, 10.0))
        assertEquals(72.0, result.finalPrice, 0.001)
        assertEquals(28.0, result.totalSaved, 0.001)
    }

    @Test
    fun `no discounts leaves price untouched`() {
        val result = applyStackedDiscounts(50.0, emptyList())
        assertEquals(50.0, result.finalPrice, 0.001)
        assertEquals(0.0, result.effectivePercent, 0.001)
    }

    @Test
    fun `zero original price does not divide by zero`() {
        val result = applyStackedDiscounts(0.0, listOf(50.0))
        assertEquals(0.0, result.effectivePercent, 0.001)
    }
}
