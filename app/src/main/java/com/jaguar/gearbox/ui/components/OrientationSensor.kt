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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jaguar.gearbox.logic.OrientationSnapshot
import com.jaguar.gearbox.logic.smoothOrientation

enum class OrientationStatus { WAITING, UNAVAILABLE, FAILED, UNRELIABLE, READY }
data class OrientationState(
    val snapshot: OrientationSnapshot? = null,
    val status: OrientationStatus = OrientationStatus.WAITING,
)

/** Distinguish missing hardware, a failed registration and an initial wait; stop sensors off-screen. */
@Composable
fun rememberOrientationState(): OrientationState {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var state by remember { mutableStateOf(OrientationState()) }
    DisposableEffect(context, lifecycle) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        var registered = false
        var smoothed: OrientationSnapshot? = null
        var reliable = true
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                try {
                    val rotation = FloatArray(9)
                    val angles = FloatArray(3)
                    SensorManager.getRotationMatrixFromVector(rotation, event.values)
                    SensorManager.getOrientation(rotation, angles)
                    if (angles.any { !it.isFinite() }) {
                        state = OrientationState(status = OrientationStatus.FAILED)
                        return
                    }
                    val raw = OrientationSnapshot(angles[0], angles[1], angles[2])
                    val previous = smoothed
                    val next = if (previous == null) raw else smoothOrientation(previous, raw) ?: previous
                    smoothed = next
                    state = OrientationState(next, if (reliable) OrientationStatus.READY else OrientationStatus.UNRELIABLE)
                } catch (_: IllegalArgumentException) {
                    state = OrientationState(status = OrientationStatus.FAILED)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                reliable = accuracy != SensorManager.SENSOR_STATUS_UNRELIABLE
                state = state.copy(status = when {
                    !reliable -> OrientationStatus.UNRELIABLE
                    state.snapshot == null -> OrientationStatus.WAITING
                    else -> OrientationStatus.READY
                })
            }
        }
        fun start() {
            if (registered) return
            if (manager == null || sensor == null) {
                state = OrientationState(status = OrientationStatus.UNAVAILABLE)
                return
            }
            state = OrientationState()
            smoothed = null
            reliable = true
            registered = runCatching {
                manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
            }.getOrDefault(false)
            if (!registered) state = OrientationState(status = OrientationStatus.FAILED)
        }
        fun stop() {
            if (registered) manager?.unregisterListener(listener)
            registered = false
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
