package com.jaguar.gearbox.logic

import org.junit.Assert.*
import org.junit.Test

class ClockMemoryTest {
    private val setup = ClockSetup("TIMER", "25", "0")
    @Test fun implicitRememberedModeCannotOverrideNotificationTarget() {
        assertFalse(mayImportClockMode(explicitImport = false))
        assertTrue(mayImportClockMode(explicitImport = true))
        val targeted = ClockState(mode = "TIMER")
        val result = if (mayImportClockMode(false)) restoreClockSetup(targeted, ClockSetup("STOPWATCH", "5", "0")) else targeted
        assertEquals("TIMER", result!!.mode)
    }
    @Test fun explicitClockNavigationAlwaysWinsOverPreferences() {
        listOf("TIMER", "STOPWATCH", "RESET").forEach { target ->
            val state = ClockState(mode = if (target == "TIMER") "TIMER" else "STOPWATCH", minutesInput = "12")
            assertEquals(state, applyClockPreferences(state, target, false, false, setup))
            assertEquals(state, applyClockPreferences(state, target, false, true, setup))
        }
        val state = ClockState(mode = "TIMER")
        assertEquals(state, applyClockPreferences(state, null, true, false, setup))
    }
    @Test fun targetArrivingDuringPreferenceReadWinsAtApplyTime() {
        val state = ClockState(mode = "TIMER", minutesInput = "12")
        val navigation = java.util.concurrent.atomic.AtomicReference<String?>(null)
        val loadedPreference = setup
        navigation.set("TIMER") // Intent arrives after preferences were read, before repository apply.
        assertEquals(state, applyClockPreferences(state, navigation.get(), false, false, loadedPreference))
        assertEquals(state, applyClockPreferences(state, navigation.get(), false, true, loadedPreference))
    }
    @Test fun ordinaryIdleLaunchHonorsMemorySetting() {
        val state = ClockState(mode = "TIMER", minutesInput = "12")
        val remembered = applyClockPreferences(state, null, false, true, setup)
        assertEquals("TIMER", remembered.mode)
        assertEquals("25", remembered.minutesInput)
        val fresh = applyClockPreferences(state, null, false, false, setup)
        assertEquals("STOPWATCH", fresh.mode)
        assertEquals("5", fresh.minutesInput)
        assertEquals(state, applyClockPreferences(state, null, false, true, null))
    }
    @Test fun progressAndCompletedClocksRemainAuthoritative() {
        listOf(ClockState(timerFinished = true), ClockState(timerRunning = true),
            ClockState(stopwatchRunning = true), ClockState(timerRemainingMillis = 1000),
            ClockState(stopwatchElapsedMillis = 1000)).forEach { state ->
            assertEquals(state, applyClockPreferences(state, null, false, false, setup))
            assertEquals(state, applyClockPreferences(state, null, false, true, setup))
        }
    }
    @Test fun rejectsInvalidOrIncompleteSetup() {
        assertNull(parseClockSetup(emptyMap()))
        assertNull(parseClockSetup(mapOf("mode" to "TIMER", "minutes" to "0", "seconds" to "0")))
        assertNull(parseClockSetup(mapOf("mode" to "TIMER", "minutes" to "1000", "seconds" to "0")))
        assertNull(parseClockSetup(mapOf("mode" to "TIMER", "minutes" to "1", "seconds" to "60")))
    }
    @Test fun importsOnlySetupWithoutStartingOrChangingNotifications() {
        val before = ClockState(ongoingNotificationEnabled = true)
        val restored = restoreClockSetup(before, setup)!!
        assertEquals("25", restored.minutesInput)
        assertEquals("TIMER", restored.mode)
        assertFalse(restored.timerRunning)
        assertFalse(restored.stopwatchRunning)
        assertEquals(0L, restored.timerRemainingMillis)
        assertTrue(restored.ongoingNotificationEnabled)
    }
    @Test fun refusesRunningAndPausedProgress() {
        listOf(ClockState(timerRunning = true), ClockState(stopwatchRunning = true),
            ClockState(timerRemainingMillis = 1000), ClockState(stopwatchElapsedMillis = 1000),
            ClockState(laps = listOf(1000))).forEach { assertNull(restoreClockSetup(it, setup)) }
    }
}
