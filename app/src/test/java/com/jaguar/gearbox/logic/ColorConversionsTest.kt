package com.jaguar.gearbox.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ColorConversionsTest {

    @Test
    fun `parses hex with and without leading hash`() {
        assertEquals(Triple(103, 80, 164), parseHex("#6750A4"))
        assertEquals(Triple(103, 80, 164), parseHex("6750A4"))
    }

    @Test
    fun `rejects malformed hex instead of throwing`() {
        assertNull(parseHex("#FF0"))
        assertNull(parseHex("#GGGGGG"))
        assertNull(parseHex(""))
    }

    @Test
    fun `black and white have no hue or saturation`() {
        assertEquals(Triple(0, 0, 0), rgbToHsl(0, 0, 0))
        assertEquals(Triple(0, 0, 100), rgbToHsl(255, 255, 255))
    }

    @Test
    fun `pure red is hue zero, fully saturated`() {
        assertEquals(Triple(0, 100, 50), rgbToHsl(255, 0, 0))
    }
}
