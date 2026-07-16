package com.jaguar.gearbox.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class OrientationTest {

    @Test
    fun `small change within deadband returns null`() {
        val previous = OrientationSnapshot(0f, 0f, 0f)
        val raw = OrientationSnapshot(0.0001f, 0f, 0f)
        assertNull(smoothOrientation(previous, raw))
    }

    @Test
    fun `large change moves partway toward raw value (exponential smoothing)`() {
        val previous = OrientationSnapshot(0f, 0f, 0f)
        val raw = OrientationSnapshot(0f, (PI / 2).toFloat(), 0f)
        val result = smoothOrientation(previous, raw)
        assertTrue(result != null)
        assertTrue(result!!.pitchRad > 0f && result.pitchRad < raw.pitchRad)
    }

    @Test
    fun `azimuth wraps the short way across the north boundary`() {
        // previous near 350 degrees, raw near 10 degrees - should move further toward 360/0, not
        // swing backward through 180.
        val previous = OrientationSnapshot(Math.toRadians(-10.0).toFloat(), 0f, 0f) // -10 deg = 350
        val raw = OrientationSnapshot(Math.toRadians(10.0).toFloat(), 0f, 0f)
        val result = smoothOrientation(previous, raw)!!
        val resultDegrees = azimuthDegrees(result.azimuthRad)
        // Should have moved toward 360/0 (e.g. ~353), not toward 180.
        assertTrue(resultDegrees > 350 || resultDegrees < 20)
    }

    @Test
    fun `azimuthDegrees normalizes to 0-360`() {
        assertEquals(0.0, azimuthDegrees(0f), 0.01)
        assertEquals(180.0, azimuthDegrees(PI.toFloat()), 0.5)
        assertEquals(350.0, azimuthDegrees(Math.toRadians(-10.0).toFloat()), 0.5)
    }

    @Test
    fun `compassPoint resolves cardinal and intercardinal directions`() {
        assertEquals("N", compassPoint(0.0))
        assertEquals("E", compassPoint(90.0))
        assertEquals("S", compassPoint(180.0))
        assertEquals("W", compassPoint(270.0))
        assertEquals("NE", compassPoint(45.0))
        assertEquals("N", compassPoint(359.0))
    }
}
