package com.jaguar.gearbox
import com.jaguar.gearbox.sensors.*
import com.jaguar.gearbox.data.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*
import java.util.Random
class PitchDetectionTest {
    private fun tone(f:Double, amplitude:Double=12000.0)=ShortArray(PITCH_WINDOW_SAMPLES) {
        (sin(2*PI*f*it/16000)*amplitude).roundToInt().toShort()
    }
    @Test fun tonesAndNumericalEndpoints() {
        for(f in listOf(65.0,80.0,110.0,220.0,440.0,880.0,1000.0)) {
            val value=detectPitch(tone(f))
            assertNotNull("Missing $f",value)
            assertEquals(f,value!!.frequencyHz,f*0.004)
            assertTrue(value.frequencyHz in 65.0..1000.0)
            assertTrue(value.confidence in 0.88..1.0)
        }
        assertEquals(65.0,detectPitch(tone(65.0))!!.frequencyHz,0.001)
        assertEquals(1000.0,detectPitch(tone(1000.0))!!.frequencyHz,0.001)
        assertEquals(69,detectPitch(tone(440.0))!!.noteNumber)
        assertEquals(25.0,detectPitch(tone(440.0*2.0.pow(25.0/1200)))!!.cents,1.0)
    }
    @Test fun unsuitableInputAndOutOfRange() {
        for(f in listOf(40.0,64.0,1001.0,1500.0,2000.0)) assertNull("Accepted $f",detectPitch(tone(f)))
        assertNull(detectPitch(ShortArray(PITCH_WINDOW_SAMPLES)))
        assertNull(detectPitch(ShortArray(PITCH_WINDOW_SAMPLES){1000}))
        assertNull(detectPitch(tone(440.0,20.0)))
        val random=Random(123)
        assertNull(detectPitch(ShortArray(PITCH_WINDOW_SAMPLES){(random.nextInt(24001)-12000).toShort()}))
        assertNull(detectPitch(ShortArray(PITCH_WINDOW_SAMPLES){if(it%2==0) Short.MIN_VALUE else Short.MAX_VALUE}))
    }
    @Test fun cancellationSourceAndPrivacy() {
        val samples=tone(440.0);val original=samples.clone()
        detectPitch(samples);assertArrayEquals(original,samples)
        var checks=0
        try { detectPitch(samples){if(++checks==2) throw IllegalStateException()};fail() }
        catch(_:IllegalStateException){assertEquals(2,checks)}
        assertFalse(ToolMemoryPolicy.permitsRoute("tool/pitch_tuner"))
        assertFalse(ToolMemoryPolicy.permitsModeRoute("tool/pitch_tuner"))
        assertFalse(canRestoreToolSnapshot("tool/pitch_tuner"))
    }
}
