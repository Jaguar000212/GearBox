package com.jaguar.gearbox.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveToolLayoutTest {
    @Test fun narrowScreensAlwaysStack() {
        assertFalse(useSideBySideToolLayout(360f, 1f))
        assertFalse(useSideBySideToolLayout(719f, 1f))
    }
    @Test fun wideScreensSplitOnlyWithComfortableTextSize() {
        assertTrue(useSideBySideToolLayout(720f, 1f))
        assertTrue(useSideBySideToolLayout(808f, 1.3f))
        assertFalse(useSideBySideToolLayout(808f, 1.5f))
        assertFalse(useSideBySideToolLayout(1200f, 2f))
    }
}
