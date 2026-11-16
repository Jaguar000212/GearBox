package com.jaguar.gearbox

import com.jaguar.gearbox.logic.ClockState
import com.jaguar.gearbox.logic.advanceClockState
import com.jaguar.gearbox.logic.homeClockSummary
import org.junit.Assert.*
import org.junit.Test

class HomeClockSummaryTest {
    @Test fun idleClockHasNoShortcut() {
        assertNull(homeClockSummary(ClockState()))
    }

    @Test fun runningAndPausedClocksRemainDistinct() {
        val state = ClockState(timerRemainingMillis = 1000, stopwatchElapsedMillis = 2000,
            stopwatchRunning = true)
        assertEquals("Timer paused · Stopwatch running", homeClockSummary(state))
        assertEquals("Timer running · Stopwatch paused", homeClockSummary(state.copy(
            timerRunning = true, stopwatchRunning = false)))
        // Presentation cannot alter the original state or selected mode.
        assertEquals("STOPWATCH", state.mode)
        assertFalse(state.timerRunning)
    }

    @Test fun elapsedCountdownIsFinishedNotPaused() {
        val state = ClockState(timerRunning = true, timerRemainingMillis = 1000,
            savedAt = 1000, savedElapsed = 1000, bootCount = 1)
        val snapshot = advanceClockState(state, 3000, 3000, 1)
        assertEquals("Timer finished", homeClockSummary(snapshot))
        assertTrue(state.timerRunning)
    }
}
