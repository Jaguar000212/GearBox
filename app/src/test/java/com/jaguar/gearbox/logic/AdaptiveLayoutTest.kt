package com.jaguar.gearbox.logic

import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveLayoutTest {
    @Test fun `compact windows never force two cramped columns`() {
        assertEquals(1, adaptiveToolColumnCount(320f, 1f))
        assertEquals(2, adaptiveToolColumnCount(360f, 1f))
        assertEquals(2, adaptiveToolColumnCount(411f, 1f))
    }

    @Test fun `larger fonts reduce columns instead of clipping cards`() {
        assertEquals(1, adaptiveToolColumnCount(411f, 1.5f))
        assertEquals(1, adaptiveToolColumnCount(600f, 2f))
    }

    @Test fun `large windows gain columns with a sensible maximum`() {
        assertEquals(3, adaptiveToolColumnCount(600f, 1f))
        assertEquals(4, adaptiveToolColumnCount(840f, 1f))
        assertEquals(4, adaptiveToolColumnCount(2000f, 1f))
    }

    @Test fun `invalid constraints fall back to one column`() {
        assertEquals(1, adaptiveToolColumnCount(0f, 1f))
        assertEquals(1, adaptiveToolColumnCount(Float.POSITIVE_INFINITY, 1f))
        assertEquals(2, adaptiveToolColumnCount(360f, Float.NaN))
    }
}
