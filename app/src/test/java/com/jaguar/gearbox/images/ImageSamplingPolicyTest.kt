package com.jaguar.gearbox.images

import org.junit.Assert.*
import org.junit.Test

class ImageSamplingPolicyTest {
    @Test fun dimensionsAndDownsamplingAreBounded() {
        assertFalse(ImageSamplingLimits.validSource(0, 1))
        assertFalse(ImageSamplingLimits.validSource(12001, 1))
        assertFalse(ImageSamplingLimits.validSource(10000, 10000))
        assertTrue(ImageSamplingLimits.validSource(8000, 5000))
        assertEquals(8, ImageSamplingLimits.sampleSize(8000, 5000))
        assertEquals(1, ImageSamplingLimits.sampleSize(1024, 1))
        assertEquals(2, ImageSamplingLimits.sampleSize(1025, 1))
    }
    @Test fun tapMatchesDisplayedPixelIncludingEdgesAndRejectsInvalidCoordinates() {
        assertEquals(0 to 0, displayedPixel(0f, 0f, 200, 100, 20, 10))
        assertEquals(10 to 5, displayedPixel(100f, 50f, 200, 100, 20, 10))
        assertEquals(19 to 9, displayedPixel(199.9f, 99.9f, 200, 100, 20, 10))
        assertNull(displayedPixel(200f, 50f, 200, 100, 20, 10))
        assertNull(displayedPixel(Float.NaN, 0f, 200, 100, 20, 10))
        assertNull(displayedPixel(-1f, 0f, 200, 100, 20, 10))
    }
}
