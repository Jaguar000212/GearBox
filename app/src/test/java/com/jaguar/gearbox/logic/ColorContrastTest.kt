package com.jaguar.gearbox.logic

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.pow

class ColorContrastTest {
    private val black = RgbColor(0, 0, 0)
    private val white = RgbColor(255, 255, 255)
    @Test fun blackWhiteAndPrimaryLuminance() {
        assertEquals(0.0, relativeLuminance(black), 1e-12)
        assertEquals(1.0, relativeLuminance(white), 1e-12)
        assertEquals(0.2126, relativeLuminance(255, 0, 0), 1e-12)
        assertEquals(0.7152, relativeLuminance(0, 255, 0), 1e-12)
        assertEquals(0.0722, relativeLuminance(0, 0, 255), 1e-12)
        assertEquals(21.0, contrastRatio(black, white), 1e-12)
        assertEquals(1.0, contrastRatio(white, white), 1e-12)
    }
    @Test fun transferFunctionUsesCorrectBranches() {
        assertEquals((10.0 / 255) / 12.92, relativeLuminance(10, 10, 10), 1e-12)
        assertEquals(((11.0 / 255 + 0.055) / 1.055).pow(2.4), relativeLuminance(11, 11, 11), 1e-12)
        assertEquals(0.21586050011389926, relativeLuminance(128, 128, 128), 1e-12)
    }
    @Test fun ratioIsSymmetricAndBounded() {
        for (r in 0..255 step 51) for (g in 0..255 step 51) for (b in 0..255 step 51) {
            val color = RgbColor(r, g, b)
            val ratio = contrastRatio(color, white)
            assertTrue(ratio in 1.0..21.0)
            assertEquals(ratio, contrastRatio(white, color), 0.0)
            assertEquals(1.0, contrastRatio(color, color), 0.0)
        }
    }
    @Test fun thresholdsUseUnroundedRatios() {
        assertFalse(meetsAaNormalText(4.49999))
        assertTrue(meetsAaNormalText(4.5))
        assertFalse(meetsAaLargeText(2.99999))
        assertTrue(meetsAaLargeText(3.0))
        assertFalse(meetsAaNormalText(Double.NaN))
        assertFalse(meetsAaLargeText(Double.POSITIVE_INFINITY))
        assertFalse(meetsAaNormalText(contrastRatio(RgbColor(119, 119, 119), white)))
        assertTrue(meetsAaNormalText(contrastRatio(RgbColor(118, 118, 118), white)))
    }
    @Test(expected = IllegalArgumentException::class)
    fun invalidRgbIsRejected() { relativeLuminance(-1, 0, 0) }
}
