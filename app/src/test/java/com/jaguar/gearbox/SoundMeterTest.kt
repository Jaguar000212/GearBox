package com.jaguar.gearbox

import com.jaguar.gearbox.sensors.*
import com.jaguar.gearbox.data.*
import org.junit.Assert.*
import org.junit.Test

class SoundMeterTest {
    @Test fun silenceAndFullScaleAndHalfScale() {
        val silence = soundLevels(ShortArray(SOUND_WINDOW_SAMPLES))
        assertEquals(Double.NEGATIVE_INFINITY, silence.rmsDbfs, 0.0)
        assertEquals(Double.NEGATIVE_INFINITY, silence.peakDbfs, 0.0)
        assertEquals(0.0, soundLevels(ShortArray(SOUND_WINDOW_SAMPLES) { Short.MIN_VALUE }).rmsDbfs, 0.0)
        val samples = ShortArray(SOUND_WINDOW_SAMPLES) { 16384 }
        val original = samples.clone()
        val level = soundLevels(samples)
        assertEquals(-6.020599913, level.rmsDbfs, 1e-8)
        assertEquals(level.rmsDbfs, level.peakDbfs, 0.0)
        assertArrayEquals(original, samples)
    }
    @Test fun noRetentionEvenWithExplicitOverrides() {
        val route = "tool/sound_level_meter"
        assertFalse(ToolMemoryPolicy.permitsRoute(route))
        assertFalse(ToolMemoryPolicy.permitsModeRoute(route))
        assertFalse(canRestoreToolSnapshot(route))
        val settings = ToolMemorySettings(historyEnabled=true, rememberResults=true,
            automaticMemory=mapOf(route to ToolAutomaticMemory(true,true)))
        assertFalse(settings.permitsHistory(route))
        assertFalse(settings.permitsResult(route))
    }
}
