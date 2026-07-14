package com.jaguar.gearbox.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerCatchUpTest {

    @Test
    fun `no savedAt leaves state untouched`() {
        val result = catchUpTimerState(
            stopwatchElapsedMillis = 1000L,
            stopwatchRunning = true,
            timerRemainingMillis = 5000L,
            timerRunning = true,
            timerFinished = false,
            savedAt = 0L,
            now = 999_999L,
        )
        assertEquals(1000L, result.stopwatchElapsedMillis)
        assertEquals(5000L, result.timerRemainingMillis)
        assertTrue(result.timerRunning)
    }

    @Test
    fun `running stopwatch advances by the elapsed gap`() {
        val result = catchUpTimerState(
            stopwatchElapsedMillis = 1000L,
            stopwatchRunning = true,
            timerRemainingMillis = 0L,
            timerRunning = false,
            timerFinished = false,
            savedAt = 10_000L,
            now = 13_000L,
        )
        assertEquals(4000L, result.stopwatchElapsedMillis)
    }

    @Test
    fun `paused stopwatch does not advance`() {
        val result = catchUpTimerState(
            stopwatchElapsedMillis = 1000L,
            stopwatchRunning = false,
            timerRemainingMillis = 0L,
            timerRunning = false,
            timerFinished = false,
            savedAt = 10_000L,
            now = 13_000L,
        )
        assertEquals(1000L, result.stopwatchElapsedMillis)
    }

    @Test
    fun `running timer counts down and finishes exactly at zero`() {
        val result = catchUpTimerState(
            stopwatchElapsedMillis = 0L,
            stopwatchRunning = false,
            timerRemainingMillis = 3000L,
            timerRunning = true,
            timerFinished = false,
            savedAt = 10_000L,
            now = 13_000L,
        )
        assertEquals(0L, result.timerRemainingMillis)
        assertFalse(result.timerRunning)
        assertTrue(result.timerFinished)
    }

    @Test
    fun `running timer clamps at zero when the gap overshoots`() {
        val result = catchUpTimerState(
            stopwatchElapsedMillis = 0L,
            stopwatchRunning = false,
            timerRemainingMillis = 3000L,
            timerRunning = true,
            timerFinished = false,
            savedAt = 10_000L,
            now = 60_000L,
        )
        assertEquals(0L, result.timerRemainingMillis)
        assertTrue(result.timerFinished)
    }

    @Test
    fun `formatDurationCompact drops the hour segment under an hour`() {
        assertEquals("05:09", formatDurationCompact(309_000L))
    }

    @Test
    fun `formatDurationCompact includes hours past an hour`() {
        assertEquals("1:00:05", formatDurationCompact(3_605_000L))
    }
}
