package com.jaguar.gearbox.ui.components

import android.content.Context
import android.view.Surface
import com.jaguar.gearbox.logic.DisplayGravity
import com.jaguar.gearbox.logic.normalizedDisplayGravity
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jaguar.gearbox.logic.OrientationSnapshot
import com.jaguar.gearbox.logic.smoothOrientation

enum class OrientationStatus { WAITING, UNAVAILABLE, FAILED, UNRELIABLE, READY }
data class OrientationState(
    val snapshot: OrientationSnapshot? = null,
    val status: OrientationStatus = OrientationStatus.WAITING,
    val gravity: DisplayGravity? = null,
    val displayRotation: Int? = null,
    val continuity: Long = 0L,
)

/** Distinguish missing hardware, a failed registration and an initial wait; stop sensors off-screen. */
@Composable
fun rememberOrientationState(): OrientationState {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var state by remember { mutableStateOf(OrientationState()) }
    val continuityCounter = remember { java.util.concurrent.atomic.AtomicLong() }
    DisposableEffect(context, lifecycle) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        var listener: SensorEventListener? = null
        var epoch = 0L
        var continuity = continuityCounter.get()
        var smoothed: OrientationSnapshot? = null
        var previousRotation: Int? = null
        fun stop() {
            ++epoch
            continuity = continuityCounter.incrementAndGet()
            val old = listener
            listener = null
            if (old != null) runCatching { manager?.unregisterListener(old) }
            smoothed = null
            previousRotation = null
            state = OrientationState(continuity = continuity)
        }
        fun start() {
            if (listener != null) return
            if (manager == null || sensor == null) {
                continuity = continuityCounter.incrementAndGet()
                state = OrientationState(status = OrientationStatus.UNAVAILABLE, continuity = continuity)
                return
            }
            val token = ++epoch
            continuity = continuityCounter.incrementAndGet()
            state = OrientationState(continuity = continuity)
            smoothed = null
            previousRotation = null
            val callback = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (token != epoch || listener !== this) return
                    if (!isOrientationAccuracyUsable(event.accuracy)) {
                        smoothed = null
                        continuity = continuityCounter.incrementAndGet()
                        state = OrientationState(status = OrientationStatus.UNRELIABLE, continuity = continuity)
                        return
                    }
                    try {
                        val displayRotation = context.display?.rotation ?: Surface.ROTATION_0
                        if (previousRotation != displayRotation) { smoothed = null; continuity = continuityCounter.incrementAndGet() }
                        previousRotation = displayRotation
                        val matrix = FloatArray(9)
                        SensorManager.getRotationMatrixFromVector(matrix, event.values)
                        val remapped = remapDisplayRotation(matrix, displayRotation)
                        val angles = FloatArray(3)
                        SensorManager.getOrientation(remapped, angles)
                        val gravity = normalizedDisplayGravity(remapped[6].toDouble(), remapped[7].toDouble(), remapped[8].toDouble())
                        check(angles.all { it.isFinite() } && gravity != null)
                        val raw = OrientationSnapshot(angles[0], angles[1], angles[2])
                        val next = smoothed?.let { smoothOrientation(it,raw) } ?: raw
                        smoothed = next
                        state = OrientationState(next, OrientationStatus.READY, gravity, displayRotation, continuity)
                    } catch (_: RuntimeException) {
                        smoothed = null
                        continuity = continuityCounter.incrementAndGet()
                        state = OrientationState(status = OrientationStatus.FAILED, continuity = continuity)
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                    if (token != epoch || listener !== this) return
                    if (!isOrientationAccuracyUsable(accuracy)) {
                        smoothed = null
                        continuity = continuityCounter.incrementAndGet()
                        state = OrientationState(status = OrientationStatus.UNRELIABLE, continuity = continuity)
                    } else if (state.status != OrientationStatus.READY) {
                        smoothed = null
                        state = OrientationState(continuity = continuity)
                    }
                }
            }
            listener = callback
            if (!runCatching { manager.registerListener(callback,sensor,SensorManager.SENSOR_DELAY_UI) }.getOrDefault(false)) {
                stop()
                state = OrientationState(status = OrientationStatus.FAILED, continuity = continuity)
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> start()
                Lifecycle.Event.ON_STOP -> stop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) start()
        onDispose { lifecycle.removeObserver(observer); stop() }
    }
    return state
}

/** LOW requires calibration, per https://developer.android.com/reference/android/hardware/SensorManager
 * Only MEDIUM/HIGH readings are eligible for a live estimate or capture; this is not precision certification.
 */
internal fun isOrientationAccuracyUsable(accuracy: Int): Boolean =
    accuracy == SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM || accuracy == SensorManager.SENSOR_STATUS_ACCURACY_HIGH

/** Display X points right, display Y toward the top; Z points out of the screen. */
internal fun remapDisplayRotation(matrix: FloatArray, rotation: Int): FloatArray {
    require(matrix.size == 9 && matrix.all { it.isFinite() })
    val axes = when(rotation) {
        Surface.ROTATION_0 -> SensorManager.AXIS_X to SensorManager.AXIS_Y
        Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
        Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
        Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
        else -> error("Unsupported display rotation")
    }
    return FloatArray(9).also { check(SensorManager.remapCoordinateSystem(matrix, axes.first, axes.second, it)) }
}
