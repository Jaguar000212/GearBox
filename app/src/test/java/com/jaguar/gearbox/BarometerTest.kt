package com.jaguar.gearbox
import com.jaguar.gearbox.sensors.*
import com.jaguar.gearbox.data.*
import org.junit.Assert.*
import org.junit.Test
class BarometerTest {
    @Test fun referenceAndFormulaBounds() {
        assertEquals(1013.25,parseSeaLevelReference("1013.25")!!,0.0)
        assertEquals(800.0,parseSeaLevelReference("800")!!,0.0)
        assertEquals(1100.0,parseSeaLevelReference("1100.00")!!,0.0)
        for(s in listOf("799.99","1100.01","0800","1e3","1000.001","NaN"," 1000","1000,5")) assertNull(s,parseSeaLevelReference(s))
        assertEquals(0.0,approximatePressureAltitude(1013.25,1013.25)!!,0.0)
        assertTrue(approximatePressureAltitude(900.0,1013.25)!!>0)
        assertTrue(approximatePressureAltitude(1100.0,1013.25)!!<0)
        assertNull(approximatePressureAltitude(Double.NaN,1013.25))
        assertNull(approximatePressureAltitude(99.99,1013.25))
        assertNull(approximatePressureAltitude(1200.01,1013.25))
        assertNull(approximatePressureAltitude(1000.0,1100.01))
    }
    @Test fun invalidAndStaleCallbacksCannotKeepOrReviveValues() {
        var reading:((Float,Int)->Unit)?=null
        var fail=false
        val source=object:BarometerSource {
            override val available=true
            override val maximumRange=1200f
            override val resolution=0.1f
            override fun start(onReading:(Float,Int)->Unit,onAccuracy:(Int)->Unit):Boolean {
                reading=onReading
                if(fail) {onReading(1000f,3);throw SecurityException()}
                return true
            }
            override fun stop(){}
        }
        var state=BarometerState()
        val controller=BarometerController(source){state=it}
        controller.start();val old=reading!!;old(1000f,3)
        assertEquals(1000f,state.pressureHpa!!,0f)
        old(Float.NaN,3);assertNull(state.pressureHpa)
        assertEquals(BarometerStatus.WAITING,state.status)
        controller.stop();old(1000f,3);assertEquals(BarometerStatus.STOPPED,state.status)
        controller.start();old(1000f,3);assertNull(state.pressureHpa)
        controller.stop();fail=true;controller.start();reading!!(1000f,3)
        assertEquals(BarometerStatus.REGISTRATION_FAILED,state.status);assertNull(state.pressureHpa)
    }
    @Test fun noRetention() {
        val route="tool/barometer"
        assertFalse(ToolMemoryPolicy.permitsRoute(route));assertFalse(ToolMemoryPolicy.permitsModeRoute(route))
        assertFalse(canRestoreToolSnapshot(route))
    }
}
