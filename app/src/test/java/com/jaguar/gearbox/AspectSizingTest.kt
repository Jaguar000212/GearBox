package com.jaguar.gearbox

import com.jaguar.gearbox.logic.*
import java.math.BigDecimal
import org.junit.Assert.*
import org.junit.Test

class AspectSizingTest {
    private fun size(sw: Int, sh: Int, tw: Int, th: Int, mode: AspectSizingMode, percent: String = "100") =
        calculateAspectSizing(sw, sh, tw, th, BigDecimal(percent), mode)
    @Test fun ratioOnlyNeedsNoTarget() {
        val result = size(1920, 1080, 0, 0, AspectSizingMode.RATIO)
        assertNull(result.error); assertEquals(16, result.ratioWidth); assertEquals(9, result.ratioHeight)
        assertEquals(1920, result.width); assertEquals(1080, result.height)
    }
    @Test fun widthHeightAndPercentUseHalfUp() {
        val width = size(1920, 1080, 1280, 0, AspectSizingMode.WIDTH)
        assertEquals(1280, width.width); assertEquals(720, width.height)
        val height = size(3, 2, 0, 1, AspectSizingMode.HEIGHT)
        assertEquals(2, height.width); assertEquals(1, height.height)
        val percent = size(3, 3, 0, 0, AspectSizingMode.PERCENT, "50")
        assertEquals(2, percent.width); assertEquals("50.0000", percent.scalePercent())
    }
    @Test fun fitAndFillGuaranteeBoxBoundsAndCenteredRemainders() {
        val fit = size(3, 2, 8, 8, AspectSizingMode.FIT)
        assertEquals(8, fit.width); assertEquals(5, fit.height); assertEquals(3, fit.verticalAdjustment)
        val fill = size(3, 2, 8, 8, AspectSizingMode.FILL)
        assertEquals(12, fill.width); assertEquals(8, fill.height); assertEquals(4, fill.horizontalAdjustment)
        val random = java.util.Random(42)
        repeat(500) {
            val sw = random.nextInt(500) + 1; val sh = random.nextInt(500) + 1
            val tw = random.nextInt(500) + 1; val th = random.nextInt(500) + 1
            val fitted = size(sw, sh, tw, th, AspectSizingMode.FIT)
            if (fitted.error == null) assertTrue(fitted.width <= tw && fitted.height <= th)
            val filled = size(sw, sh, tw, th, AspectSizingMode.FILL)
            if (filled.error == null) assertTrue(filled.width >= tw && filled.height >= th)
        }
    }
    @Test fun boundsAndZeroPixelResultsRejectWithoutClamping() {
        assertEquals(AspectSizingError.OUTPUT_RANGE, size(1000000, 1, 1, 1, AspectSizingMode.FIT).error)
        assertEquals(AspectSizingError.OUTPUT_RANGE, size(1000000, 1000000, 0, 0, AspectSizingMode.PERCENT, "10000").error)
        assertEquals(AspectSizingError.INVALID_INPUT, size(0, 10, 0, 0, AspectSizingMode.RATIO).error)
        assertNull(parsePixelDimension("0")); assertNull(parsePixelDimension("1000001")); assertNull(parsePixelDimension("01"))
        assertNull(parseSizingPercent("0")); assertNull(parseSizingPercent("10000.01")); assertNull(parseSizingPercent("1e2"))
        assertNotNull(parseSizingPercent("0.01")); assertNotNull(parseSizingPercent("10000.00"))
    }
}
