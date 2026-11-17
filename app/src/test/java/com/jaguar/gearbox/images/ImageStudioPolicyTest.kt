package com.jaguar.gearbox.images

import org.junit.Assert.*
import org.junit.Test

class ImageStudioPolicyTest {
    @Test fun sourceAndOutputLimitsRejectOverflowAndZero() {
        assertFalse(ImageStudioLimits.validSource(Int.MAX_VALUE, Int.MAX_VALUE))
        assertFalse(ImageStudioLimits.validSource(12001, 1))
        assertFalse(ImageStudioLimits.validSource(8000, 8000))
        assertFalse(ImageStudioLimits.validOutput(2048, 2048))
        assertFalse(ImageStudioLimits.validOutput(0, 1))
        assertTrue(ImageStudioLimits.validOutput(2000, 2000))
    }
    @Test fun decodeBoundsRespectBothEdgeAndAreaWithoutUpscaling() {
        listOf(12000 to 1, 6000 to 6000, 4000 to 10000, 1 to 12000, 100 to 50).forEach { (w, h) ->
            val (dw, dh) = ImageStudioLimits.decodedSize(w, h)
            assertTrue(ImageStudioLimits.validOutput(dw, dh))
            assertTrue(dw <= w && dh <= h)
        }
        assertEquals(100 to 50, ImageStudioLimits.decodedSize(100, 50))
    }
    @Test fun cropIsPositiveExclusiveAndInsideDecodedImage() {
        assertTrue(ImageStudioCrop(0, 0, 20, 10).validFor(20, 10))
        assertFalse(ImageStudioCrop(-1, 0, 20, 10).validFor(20, 10))
        assertFalse(ImageStudioCrop(1, 0, 1, 10).validFor(20, 10))
        assertFalse(ImageStudioCrop(0, 0, 21, 10).validFor(20, 10))
    }
}
