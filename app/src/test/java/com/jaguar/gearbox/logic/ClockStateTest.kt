package com.jaguar.gearbox.logic

import org.junit.Assert.*
import org.junit.Test

class ClockStateTest {
    @Test fun `monotonic elapsed time ignores wall clock adjustment within boot`() {
        val before = ClockState(timerRemainingMillis = 10_000, timerRunning = true,
            savedAt = 1_000_000, savedElapsed = 5_000, bootCount = 2)
        val after = advanceClockState(before, 500_000, 8_000, 2)
        assertEquals(7_000L, after.timerRemainingMillis)
        assertTrue(after.timerRunning)
    }

    @Test fun `reboot uses elapsed wall time to restore timer`() {
        val before = ClockState(timerRemainingMillis = 10_000, timerRunning = true,
            savedAt = 1_000_000, savedElapsed = 50_000, bootCount = 2)
        val after = advanceClockState(before, 1_004_000, 500, 3)
        assertEquals(6_000L, after.timerRemainingMillis)
    }

    @Test fun `expired timer completes once and rejects stale alarms`() {
        val state = ClockState(timerRemainingMillis = 1000, timerRunning = true,
            savedAt = 1000, generation = 4)
        val finished = advanceClockState(state, 3000, 100, 1)
        assertFalse(finished.timerRunning)
        assertTrue(finished.timerFinished)
        assertNull(claimTimerCompletion(finished, 3))
        val claimed = claimTimerCompletion(finished, 4)!!
        assertEquals(4L, claimed.alertedGeneration)
        assertNull(claimTimerCompletion(claimed, 4))
    }

    @Test fun `old alarm cannot complete a newer timer session`() {
        val newer = ClockState(timerRemainingMillis = 0, timerFinished = true, generation = 5)
        assertNull(claimTimerCompletion(newer, 4))
        assertNotNull(claimTimerCompletion(newer, 5))
    }

    @Test fun `start pause resume and reset produce scheduling states`() {
        val started = toggleCountdown(ClockState(minutesInput = "0", secondsInput = "10"))
        assertTrue(started.timerRunning)
        assertEquals(10_000L, started.timerRemainingMillis)
        val paused = toggleCountdown(started)
        assertFalse(paused.timerRunning)
        assertEquals(10_000L, paused.timerRemainingMillis)
        val resumed = toggleCountdown(paused)
        assertTrue(resumed.timerRunning)
        assertTrue(resumed.generation > started.generation)
        val reset = resetCountdown(resumed)
        assertFalse(reset.timerRunning)
        assertFalse(reset.timerFinished)
        assertEquals(0L, reset.timerRemainingMillis)
        assertNull(claimTimerCompletion(reset, resumed.generation))
    }

    @Test fun `widget cannot start an unconfigured timer`() {
        assertFalse(toggleCountdown(ClockState(), allowNewDuration = false).timerRunning)
    }

    @Test fun `paused and reset timers never emit completion`() {
        val paused = ClockState(timerRemainingMillis = 1000, generation = 2, savedAt = 1000)
        assertEquals(1000L, advanceClockState(paused, 9000, 9000, 1).timerRemainingMillis)
        assertNull(claimTimerCompletion(paused))
        assertNull(claimTimerCompletion(paused.copy(timerRemainingMillis = 0)))
    }

    @Test fun `timer and stopwatch advance together without overflow`() {
        val state = ClockState(stopwatchElapsedMillis = Long.MAX_VALUE - 1, stopwatchRunning = true,
            timerRemainingMillis = 5000, timerRunning = true, savedAt = 1000)
        val next = advanceClockState(state, 3000, 3000, 1)
        assertEquals(Long.MAX_VALUE, next.stopwatchElapsedMillis)
        assertEquals(3000L, next.timerRemainingMillis)
    }

    @Test fun `input validation rejects rather than clamps invalid durations`() {
        assertEquals(61_000L, timerDurationMillis("1", "1"))
        assertEquals(59_999_000L, timerDurationMillis("999", "59"))
        assertNull(timerDurationMillis("1000", "0"))
        assertNull(timerDurationMillis("0", "60"))
        assertNull(timerDurationMillis("0", "0"))
        assertNull(timerDurationMillis("-1", "5"))
        assertNull(timerDurationMillis("999999999999999999999999", "0"))
    }
}
