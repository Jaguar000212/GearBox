package com.jaguar.gearbox.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.jaguar.gearbox.logic.OrientationSnapshot
import com.jaguar.gearbox.logic.smoothOrientation

/**
 * Shared by the Bubble Level and Compass screens - both derive from the same fused
 * TYPE_ROTATION_VECTOR sensor (accelerometer + magnetometer + gyroscope where available) rather
 * than each smoothing raw accelerometer/magnetometer readings separately, which is noisier and can
 * drift out of sync between the two screens.
 *
 * Returns null while no sensor reading has arrived yet, or if the device has no rotation-vector
 * sensor at all - callers should show a "sensor not available" message in that case.
 */
@Composable
fun rememberOrientationSnapshot(): OrientationSnapshot? {
    val context = LocalContext.current
    var snapshot by remember { mutableStateOf<OrientationSnapshot?>(null) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        var smoothed: OrientationSnapshot? = null

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val rotationMatrix = FloatArray(9)
                val orientation = FloatArray(3)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientation)
                val raw = OrientationSnapshot(orientation[0], orientation[1], orientation[2])

                val previous = smoothed
                val next = if (previous == null) raw else smoothOrientation(previous, raw) ?: previous
                smoothed = next
                snapshot = next
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        if (rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_GAME)
        }

        onDispose { sensorManager.unregisterListener(listener) }
    }

    return snapshot
}
