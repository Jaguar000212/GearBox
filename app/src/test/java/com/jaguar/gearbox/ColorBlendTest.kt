package com.jaguar.gearbox

import com.jaguar.gearbox.logic.*
import org.junit.Assert.*
import org.junit.Test

class ColorBlendTest {
    @Test fun strictOpaqueHexParsing() {
        assertEquals(RgbColor(18, 171, 255), parseOpaqueHex("#12abFF"))
        for (invalid in listOf("", "123456", "#fff", "#12345678", "#GG0000", " #123456", "#123456\n")) {
            assertNull(invalid, parseOpaqueHex(invalid))
        }
    }
    @Test fun endpointsPreserveExactColors() {
        val a = RgbColor(12, 99, 201); val b = RgbColor(255, 11, 0)
        assertEquals(a, blendEncodedSrgb(a, b, 0))
        assertEquals(b, blendEncodedSrgb(a, b, 100))
    }
    @Test fun midpointIsEncodedNotLinearLightAndRoundsHalfUp() {
        assertEquals(RgbColor(128, 128, 128), blendEncodedSrgb(RgbColor(0, 0, 0), RgbColor(255, 255, 255), 50))
        assertEquals(RgbColor(128, 0, 128), blendEncodedSrgb(RgbColor(255, 0, 0), RgbColor(0, 0, 255), 50))
        assertEquals(RgbColor(191, 0, 64), blendEncodedSrgb(RgbColor(255, 0, 0), RgbColor(0, 0, 255), 25))
        assertEquals("#BF0040", colorBlendHex(RgbColor(191, 0, 64)))
    }
    @Test fun complementaryWeightsAreSymmetric() {
        val a = RgbColor(17, 42, 255); val b = RgbColor(230, 51, 0)
        for (weight in 0..100) assertEquals(blendEncodedSrgb(a, b, weight), blendEncodedSrgb(b, a, 100 - weight))
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsNegativeWeight() { blendEncodedSrgb(RgbColor(0,0,0), RgbColor(0,0,0), -1) }
    @Test(expected = IllegalArgumentException::class) fun rejectsWeightAbove100() { blendEncodedSrgb(RgbColor(0,0,0), RgbColor(0,0,0), 101) }
}
