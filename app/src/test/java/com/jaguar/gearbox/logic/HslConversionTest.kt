package com.jaguar.gearbox.logic

import org.junit.Assert.*
import org.junit.Test

class HslConversionTest {
    @Test fun primaryColorsAndHueWrap() {
        assertEquals(Triple(255, 0, 0), hslToRgb(0.0, 100.0, 50.0))
        assertEquals(Triple(255, 0, 0), hslToRgb(360.0, 100.0, 50.0))
        assertEquals(Triple(0, 255, 0), hslToRgb(120.0, 100.0, 50.0))
        assertEquals(Triple(0, 0, 255), hslToRgb(240.0, 100.0, 50.0))
        assertEquals(Triple(255, 255, 0), hslToRgb(60.0, 100.0, 50.0))
        assertEquals(Triple(0, 255, 255), hslToRgb(180.0, 100.0, 50.0))
        assertEquals(Triple(255, 0, 255), hslToRgb(300.0, 100.0, 50.0))
    }
    @Test fun achromaticExtremesAndDecimalInputs() {
        assertEquals(Triple(128, 128, 128), hslToRgb(42.0, 0.0, 50.0))
        assertEquals(Triple(0, 0, 0), hslToRgb(180.0, 100.0, 0.0))
        assertEquals(Triple(255, 255, 255), hslToRgb(180.0, 100.0, 100.0))
        assertNotNull(hslToRgb(213.5, 23.7, 64.9))
    }
    @Test fun invalidInputsAreRejectedNotClamped() {
        listOf(-1.0, 361.0, Double.NaN, Double.POSITIVE_INFINITY).forEach { assertNull(hslToRgb(it, 50.0, 50.0)) }
        listOf(-1.0, 101.0, Double.NaN, Double.NEGATIVE_INFINITY).forEach {
            assertNull(hslToRgb(100.0, it, 50.0))
            assertNull(hslToRgb(100.0, 50.0, it))
        }
    }
    @Test fun roundedHslRoundTripStaysCloseToOriginalRgb() {
        for (r in 0..255 step 17) for (g in 0..255 step 17) for (b in 0..255 step 17) {
            val hsl = rgbToHsl(r, g, b)
            val rgb = hslToRgb(hsl.first.toDouble(), hsl.second.toDouble(), hsl.third.toDouble())!!
            assertTrue(kotlin.math.abs(r - rgb.first) <= 4)
            assertTrue(kotlin.math.abs(g - rgb.second) <= 4)
            assertTrue(kotlin.math.abs(b - rgb.third) <= 4)
        }
    }
}
