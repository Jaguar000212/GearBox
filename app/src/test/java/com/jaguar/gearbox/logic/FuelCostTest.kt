package com.jaguar.gearbox.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FuelCostTest {

    @Test
    fun `computes fuel needed and total cost`() {
        val result = calculateFuelCost(distance = 300.0, mileage = 15.0, pricePerUnit = 100.0)
        assertEquals(20.0, result!!.fuelNeeded, 0.001)
        assertEquals(2000.0, result.totalCost, 0.001)
    }

    @Test
    fun `zero or negative mileage is rejected`() {
        assertNull(calculateFuelCost(300.0, 0.0, 100.0))
        assertNull(calculateFuelCost(300.0, -5.0, 100.0))
    }
}
