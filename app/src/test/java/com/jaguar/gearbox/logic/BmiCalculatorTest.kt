package com.jaguar.gearbox.logic

import org.junit.Assert.assertEquals
import org.junit.Test

class BmiCalculatorTest {

    @Test
    fun `known height and weight produce expected bmi and category`() {
        // 70kg at 1.75m -> BMI 22.9, Normal weight.
        val (value, category) = computeBmi(heightM = 1.75, weightKg = 70.0)
        assertEquals("22.9", value)
        assertEquals("Normal weight", category)
    }

    @Test
    fun `category boundaries`() {
        assertEquals("Underweight", computeBmi(heightM = 1.0, weightKg = 18.4).second)
        assertEquals("Normal weight", computeBmi(heightM = 1.0, weightKg = 18.5).second)
        assertEquals("Normal weight", computeBmi(heightM = 1.0, weightKg = 24.9).second)
        assertEquals("Overweight", computeBmi(heightM = 1.0, weightKg = 25.0).second)
        assertEquals("Overweight", computeBmi(heightM = 1.0, weightKg = 29.9).second)
        assertEquals("Obese", computeBmi(heightM = 1.0, weightKg = 30.0).second)
    }
}
