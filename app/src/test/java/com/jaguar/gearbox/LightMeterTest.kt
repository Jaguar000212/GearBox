package com.jaguar.gearbox
import com.jaguar.gearbox.sensors.*
import com.jaguar.gearbox.data.*
import org.junit.Assert.*
import org.junit.Test
class LightMeterTest {
    private class Fake(override val available:Boolean=true,var succeeds:Boolean=true):LightMeterSource {
        override val maximumRange=10000f
        override val resolution=0.5f
        var starts=0; var stops=0
        lateinit var reading:(Float,Int)->Unit
        lateinit var accuracy:(Int)->Unit
        override fun start(onReading:(Float,Int)->Unit,onAccuracy:(Int)->Unit):Boolean {
            starts++; reading=onReading; accuracy=onAccuracy; return succeeds
        }
        override fun stop() { stops++ }
    }
    @Test fun lifecycleAndEpochFencePreventOldReadingsReappearing() {
        val source=Fake(); var state=LightMeterState()
        val controller=LightMeterController(source){state=it}
        controller.start(); controller.start()
        assertEquals(1,source.starts); assertEquals(LightMeterStatus.WAITING,state.status)
        val old=source.reading
        old(12f,3); assertEquals(12f,state.lux)
        controller.stop(); old(20f,3)
        assertNull(state.lux); assertEquals(LightMeterStatus.STOPPED,state.status)
        controller.start(); old(30f,3)
        assertNull(state.lux); assertEquals(LightMeterStatus.WAITING,state.status)
        source.reading(0f,0)
        assertEquals(0f,state.lux); assertEquals(LightMeterAccuracy.UNRELIABLE,state.accuracy)
        source.reading(Float.NaN,2)
        assertNull(state.lux); assertEquals(LightMeterStatus.WAITING,state.status)
        controller.stop(); assertEquals(2,source.stops)
    }
    @Test fun absentHardwareAndRegistrationFailureAreDifferent() {
        val absent=Fake(available=false); var state=LightMeterState()
        LightMeterController(absent){state=it}.start()
        assertEquals(LightMeterStatus.UNAVAILABLE,state.status); assertEquals(0,absent.starts)
        val failed=Fake(succeeds=false)
        LightMeterController(failed){state=it}.start()
        assertEquals(LightMeterStatus.REGISTRATION_FAILED,state.status); assertEquals(1,failed.stops)
        failed.reading(25f,3); assertNull(state.lux)
    }
    @Test fun thrownRegistrationAndCleanupStillFenceCallbacksAndReportFailure() {
        var reading: ((Float, Int) -> Unit)? = null
        var accuracy: ((Int) -> Unit)? = null
        var stops = 0
        var state = LightMeterState()
        val source = object : LightMeterSource {
            override val available = true
            override val maximumRange = 1000f
            override val resolution = 1f
            override fun start(onReading: (Float, Int) -> Unit, onAccuracy: (Int) -> Unit): Boolean {
                reading = onReading; accuracy = onAccuracy
                onReading(12f, 3)
                throw SecurityException("Registration denied")
            }
            override fun stop() { stops++; throw IllegalStateException("Cleanup failed") }
        }
        val controller = LightMeterController(source) { state = it }
        controller.start()
        assertEquals(LightMeterStatus.REGISTRATION_FAILED, state.status)
        assertNull(state.lux)
        assertEquals(1, stops)
        reading!!(50f, 3); accuracy!!(3)
        assertEquals(LightMeterStatus.REGISTRATION_FAILED, state.status)
        assertNull(state.lux)
        assertEquals(LightMeterAccuracy.UNKNOWN, state.accuracy)
        controller.stop()
        assertEquals(LightMeterStatus.STOPPED, state.status)
        assertEquals(1, stops)
    }
    @Test fun finiteNonnegativeLuxAndNoMemory() {
        listOf(Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,-1f).forEach { assertFalse(validLightLux(it)) }
        assertTrue(validLightLux(0f)); assertTrue(validLightLux(Float.MAX_VALUE))
        assertEquals(LightMeterAccuracy.UNKNOWN,lightMeterAccuracy(-1))
        assertFalse(ToolMemoryPolicy.permitsRoute("tool/light_meter"))
        assertFalse(ToolMemoryPolicy.permitsModeRoute("tool/light_meter"))
        assertFalse(canRestoreToolSnapshot("tool/light_meter"))
        val settings=ToolMemorySettings(historyEnabled=true,rememberResults=true,
            automaticMemory=mapOf("tool/light_meter" to ToolAutomaticMemory(true,true)))
        assertFalse(settings.permitsHistory("tool/light_meter")); assertFalse(settings.permitsResult("tool/light_meter"))
    }
}
