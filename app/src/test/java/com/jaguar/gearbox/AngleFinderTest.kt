package com.jaguar.gearbox
import com.jaguar.gearbox.logic.*
import org.junit.Assert.*
import org.junit.Test
class AngleFinderTest {
    @Test fun flatAndDisplayEdgesHaveGeometricMeaning() {
        val flat=DisplayGravity(0.0,0.0,1.0)
        assertEquals(0.0,angleFinderDegrees(flat,AngleFinderAxis.FLAT)!!,1e-8)
        assertEquals(0.0,angleFinderDegrees(DisplayGravity(0.0,0.0,-1.0),AngleFinderAxis.FLAT)!!,1e-8)
        assertEquals(90.0,angleFinderDegrees(DisplayGravity(0.0,1.0,0.0),AngleFinderAxis.FLAT)!!,1e-8)
        assertEquals(90.0,angleFinderDegrees(DisplayGravity(0.0,1.0,0.0),AngleFinderAxis.SCREEN_Y)!!,1e-8)
        assertEquals(-90.0,angleFinderDegrees(DisplayGravity(-1.0,0.0,0.0),AngleFinderAxis.SCREEN_X)!!,1e-8)
        assertEquals(45.0,angleFinderDegrees(DisplayGravity(0.0,2.0,2.0),AngleFinderAxis.SCREEN_Y)!!,1e-8)
        assertNull(normalizedDisplayGravity(0.0,0.0,0.0))
        assertNull(angleFinderDegrees(DisplayGravity(Double.NaN,0.0,1.0),AngleFinderAxis.FLAT))
    }
    @Test fun shortestSignedDeltaWrapsAndRejectsInvalidNumbers() {
        assertEquals(2.0,shortestAngleDelta(-179.0,179.0)!!,0.0)
        assertEquals(-2.0,shortestAngleDelta(179.0,-179.0)!!,0.0)
        assertEquals(-180.0,shortestAngleDelta(180.0,0.0)!!,0.0)
        assertEquals(0.0,shortestAngleDelta(720.0,0.0)!!,0.0)
        assertNull(shortestAngleDelta(Double.POSITIVE_INFINITY,0.0))
    }
}
