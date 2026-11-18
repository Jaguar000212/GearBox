package com.jaguar.gearbox.ui.components

import android.content.Context
import android.hardware.*
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jaguar.gearbox.sensors.*

private class AndroidSensorCheckerSource(context:Context):SensorCheckerSource {
    private val manager=context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private fun sensor(kind:CheckedSensor)=runCatching{manager?.getDefaultSensor(kind.androidType)}.getOrNull()
    private var listener:SensorEventListener?=null
    override fun metadata(kind:CheckedSensor):CheckedSensorMetadata {
        val sensor=sensor(kind)?:return CheckedSensorMetadata(false)
        return CheckedSensorMetadata(true,sensor.maximumRange.takeIf{it.isFinite() && it>0},sensor.resolution.takeIf{it.isFinite() && it>0})
    }
    override fun start(kind:CheckedSensor,reading:(List<Float>,Int)->Unit,accuracy:(Int)->Unit):Boolean {
        stop();val sensor=sensor(kind)?:return false
        val callback=object:SensorEventListener {
            override fun onSensorChanged(event:SensorEvent){if(event.sensor.type==kind.androidType)reading(event.values.take(4),event.accuracy)}
            override fun onAccuracyChanged(sensor:Sensor?,value:Int){if(sensor?.type==kind.androidType)accuracy(value)}
        };listener=callback
        return manager?.registerListener(callback,sensor,SensorManager.SENSOR_DELAY_UI,Handler(Looper.getMainLooper()))==true
    }
    override fun stop(){listener?.let{manager?.unregisterListener(it)};listener=null}
}
data class SensorCheckerHandle(val state:SensorCheckState,val inventory:Map<CheckedSensor,CheckedSensorMetadata>,val start:(CheckedSensor)->Unit,val stop:()->Unit)
@Composable fun rememberSensorChecker(source:SensorCheckerSource?=null):SensorCheckerHandle {
    val context=LocalContext.current;val lifecycle=LocalLifecycleOwner.current.lifecycle
    val actual=remember(context,source){source?:AndroidSensorCheckerSource(context.applicationContext)}
    var state by remember(actual){mutableStateOf(SensorCheckState())}
    val controller=remember(actual){SensorCheckerController(actual){state=it}}
    val inventory=remember(actual){CheckedSensor.entries.associateWith{actual.metadata(it)}}
    var alive by remember{mutableStateOf(true)}
    DisposableEffect(controller,lifecycle){
        alive=true
        val observer=LifecycleEventObserver{_,_->if(!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))controller.stop()}
        lifecycle.addObserver(observer)
        onDispose{alive=false;lifecycle.removeObserver(observer);controller.stop()}
    }
    return SensorCheckerHandle(state,inventory,{if(alive && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))controller.start(it)},{controller.stop()})
}
