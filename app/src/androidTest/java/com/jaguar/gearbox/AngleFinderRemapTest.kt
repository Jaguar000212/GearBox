package com.jaguar.gearbox
import android.hardware.SensorManager
import android.view.Surface
import com.jaguar.gearbox.ui.components.remapDisplayRotation
import org.junit.Assert.*
import org.junit.Test
class AngleFinderRemapTest {
    @Test fun remappingPreservesFlatGravityAndRotatesHeading() {
        val identity=floatArrayOf(1f,0f,0f,0f,1f,0f,0f,0f,1f)
        for((rotation,heading) in listOf(Surface.ROTATION_0 to 0.0,Surface.ROTATION_90 to 90.0,
            Surface.ROTATION_180 to 180.0,Surface.ROTATION_270 to -90.0)) {
            val expectedMatrix = when (rotation) {
                Surface.ROTATION_0 -> identity
                Surface.ROTATION_90 -> floatArrayOf(0f,1f,0f,-1f,0f,0f,0f,0f,1f)
                Surface.ROTATION_180 -> floatArrayOf(-1f,0f,0f,0f,-1f,0f,0f,0f,1f)
                else -> floatArrayOf(0f,-1f,0f,1f,0f,0f,0f,0f,1f)
            }
            val matrix=remapDisplayRotation(identity,rotation)
            assertArrayEquals(expectedMatrix,matrix,0f)
            assertEquals(0f,matrix[6],0f);assertEquals(0f,matrix[7],0f);assertEquals(1f,matrix[8],0f)
            val angles=FloatArray(3);SensorManager.getOrientation(matrix,angles)
            val actual = Math.toDegrees(angles[0].toDouble())
            val wrappedDifference = ((actual - heading + 540.0) % 360.0) - 180.0
            assertEquals("Display rotation $rotation", 0.0, wrappedDifference, 0.001)
        }
        assertArrayEquals(floatArrayOf(1f,0f,0f,0f,1f,0f,0f,0f,1f),identity,0f)
    }

    @Test fun tiltedGravityAxesFollowDisplayRotationWithoutChangingInclination() {
        val tiltX = floatArrayOf(1f,0f,0f, 0f,0.8f,-0.6f, 0f,0.6f,0.8f)
        val tiltY = floatArrayOf(0.8f,0f,0.6f, 0f,1f,0f, -0.6f,0f,0.8f)
        for (source in listOf(tiltX, tiltY)) {
            val original = source.clone()
            val x = source[6]; val y = source[7]; val z = source[8]
            val expected = listOf(
                Surface.ROTATION_0 to floatArrayOf(x,y,z),
                Surface.ROTATION_90 to floatArrayOf(-y,x,z),
                Surface.ROTATION_180 to floatArrayOf(-x,-y,z),
                Surface.ROTATION_270 to floatArrayOf(y,-x,z),
            )
            for ((rotation, gravity) in expected) {
                val result = remapDisplayRotation(source,rotation)
                assertArrayEquals("Tilted gravity at rotation $rotation", gravity, result.copyOfRange(6,9), 0.00001f)
                assertEquals(1f, result[6]*result[6]+result[7]*result[7]+result[8]*result[8], 0.00001f)
            }
            assertArrayEquals(original,source,0f)
        }
    }
}
