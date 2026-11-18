package com.jaguar.gearbox
import com.jaguar.gearbox.sensors.*
import com.jaguar.gearbox.data.ToolMemoryPolicy
import org.junit.Assert.*
import org.junit.Test
class SensorCheckerTest {
    private class Fake:SensorCheckerSource {
        var callback:((List<Float>,Int)->Unit)?=null
        var available=true;var registered=true;var stops=0
        override fun metadata(kind:CheckedSensor)=CheckedSensorMetadata(available)
        override fun start(kind:CheckedSensor,reading:(List<Float>,Int)->Unit,accuracy:(Int)->Unit):Boolean {callback=reading;return registered}
        override fun stop(){stops++}
    }
    @Test fun epochRejectsOldSelectionAndStoppedCallbacks(){
        val source=Fake();var state=SensorCheckState();val controller=SensorCheckerController(source){state=it}
        controller.start(CheckedSensor.ACCELEROMETER);val old=source.callback!!
        controller.start(CheckedSensor.LIGHT);old(listOf(1f,2f,3f),3)
        assertEquals(SensorCheckStatus.WAITING,state.status)
        source.callback!!(listOf(8f),3);assertEquals(listOf(8f),state.values)
        source.callback!!(listOf(Float.NaN),3);assertTrue(state.values.isEmpty())
        controller.stop();source.callback!!(listOf(9f),3);assertEquals(SensorCheckStatus.STOPPED,state.status)
    }
    @Test fun failuresAndVectorShapeAreHonest(){
        val source=Fake();var state=SensorCheckState();val controller=SensorCheckerController(source){state=it}
        source.available=false;controller.start(CheckedSensor.LIGHT);assertEquals(SensorCheckStatus.UNAVAILABLE,state.status)
        source.available=true;source.registered=false;controller.start(CheckedSensor.LIGHT);assertEquals(SensorCheckStatus.ERROR,state.status)
        assertNull(checkedSensorValues(CheckedSensor.GYROSCOPE,listOf(1f)))
        assertNull(checkedSensorValues(CheckedSensor.LIGHT,listOf(-1f)))
        assertEquals(3,checkedSensorValues(CheckedSensor.ROTATION_VECTOR,listOf(0f,0f,0f))!!.size)
        assertEquals(4,checkedSensorValues(CheckedSensor.ROTATION_VECTOR,listOf(0f,0f,0f,1f,99f))!!.size)
        assertFalse(ToolMemoryPolicy.permitsRoute("tool/sensor_checker"))
        assertFalse(ToolMemoryPolicy.permitsModeRoute("tool/sensor_checker"))
    }
}
