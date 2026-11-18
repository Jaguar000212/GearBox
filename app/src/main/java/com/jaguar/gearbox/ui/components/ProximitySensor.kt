package com.jaguar.gearbox.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jaguar.gearbox.sensors.*

private class AndroidProximitySource(context: Context) : ProximitySource {
    private val manager=context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val sensor=runCatching { manager?.getDefaultSensor(Sensor.TYPE_PROXIMITY) }.getOrNull()
    private var listener:SensorEventListener?=null
    override val available get()=manager != null && sensor != null
    override val maximumRange get()=sensor?.maximumRange
    override val resolution get()=sensor?.resolution
    override fun start(onReading:(Float,Int)->Unit,onAccuracy:(Int)->Unit):Boolean {
        val device=sensor ?: return false
        val service=manager ?: return false
        val callback=object:SensorEventListener {
            override fun onSensorChanged(event:SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_PROXIMITY) onReading(event.values.firstOrNull() ?: Float.NaN,event.accuracy)
            }
            override fun onAccuracyChanged(sensor:Sensor?,accuracy:Int) { onAccuracy(accuracy) }
        }
        listener=callback
        return service.registerListener(callback,device,SensorManager.SENSOR_DELAY_UI,Handler(Looper.getMainLooper()))
    }
    override fun stop() { val old=listener; listener=null; old?.let { manager?.unregisterListener(it) } }
}

/** Injection seam also permits lifecycle tests without real proximity-sensor hardware. */
@Composable
fun rememberProximityState(source: ProximitySource? = null): ProximityState {
    val context=LocalContext.current
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    val actual=remember(context,source) { source ?: AndroidProximitySource(context.applicationContext) }
    var state by remember(actual) { mutableStateOf(ProximityState()) }
    DisposableEffect(actual,lifecycle) {
        val controller=ProximityController(actual) { state=it }
        val observer=LifecycleEventObserver { _,_ ->
            if(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) controller.start() else controller.stop()
        }
        lifecycle.addObserver(observer)
        if(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) controller.start()
        onDispose { lifecycle.removeObserver(observer); controller.stop() }
    }
    return state
}
