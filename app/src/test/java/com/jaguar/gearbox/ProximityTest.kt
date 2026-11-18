package com.jaguar.gearbox
import com.jaguar.gearbox.sensors.*
import com.jaguar.gearbox.data.*
import org.junit.Assert.*
import org.junit.Test
class ProximityTest {
    @Test fun binaryThresholdAndInvalidValues() {
        assertEquals(ProximityClassification.NEAR,classifyProximity(0f,5f))
        assertEquals(ProximityClassification.NEAR,classifyProximity(4.99f,5f))
        assertEquals(ProximityClassification.FAR,classifyProximity(5f,5f))
        assertEquals(ProximityClassification.FAR,classifyProximity(10f,5f))
        for(max in listOf(null,0f,-1f,Float.NaN,Float.POSITIVE_INFINITY))
            assertEquals(ProximityClassification.UNKNOWN,classifyProximity(0f,max))
        for(value in listOf(-1f,Float.NaN,Float.POSITIVE_INFINITY)) {
            assertFalse(validProximityDistance(value))
            assertEquals(ProximityClassification.UNKNOWN,classifyProximity(value,5f))
        }
    }
    @Test fun invalidStaleAndFailedCallbacksAreFenced() {
        var reading:((Float,Int)->Unit)?=null
        var fail=false
        val source=object:ProximitySource {
            override val available=true
            override val maximumRange=5f
            override val resolution=1f
            override fun start(onReading:(Float,Int)->Unit,onAccuracy:(Int)->Unit):Boolean {
                reading=onReading
                if(fail) {onReading(0f,3);throw SecurityException()}
                return true
            }
            override fun stop(){}
        }
        var state=ProximityState()
        val controller=ProximityController(source){state=it}
        controller.start();val old=reading!!;old(0f,3)
        assertEquals(ProximityClassification.NEAR,state.classification)
        old(Float.NaN,3);assertNull(state.distanceCm)
        assertEquals(ProximityClassification.UNKNOWN,state.classification)
        controller.stop();old(0f,3);assertEquals(ProximityStatus.STOPPED,state.status)
        controller.start();old(0f,3);assertNull(state.distanceCm)
        controller.stop();fail=true;controller.start();reading!!(0f,3)
        assertEquals(ProximityStatus.REGISTRATION_FAILED,state.status);assertNull(state.distanceCm)
    }
    @Test fun noRetention() {
        val route="tool/proximity_checker"
        assertFalse(ToolMemoryPolicy.permitsRoute(route));assertFalse(ToolMemoryPolicy.permitsModeRoute(route))
        assertFalse(canRestoreToolSnapshot(route))
    }
}
