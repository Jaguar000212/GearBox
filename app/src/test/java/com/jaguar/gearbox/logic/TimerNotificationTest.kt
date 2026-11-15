package com.jaguar.gearbox.logic

import org.junit.Assert.*
import org.junit.Test

class TimerNotificationTest {
    private val running = ClockState(timerRunning = true, timerRemainingMillis = 60_000,
        generation = 1, notificationRevision = 4, ongoingNotificationEnabled = true)

    @Test fun defaultsRemainQuiet() { assertFalse(ClockState().ongoingNotificationEnabled) }
    @Test fun pauseIsIdempotentAndStaleCancelCannotAffectResumedTimer() {
        val paused = prepareTimerNotificationChange(running,
            applyTimerNotificationAction(running, 4, TimerNotificationAction.PAUSE))
        assertFalse(paused.timerRunning)
        assertEquals(60_000L, paused.timerRemainingMillis)
        assertEquals(paused, applyTimerNotificationAction(paused, 4, TimerNotificationAction.PAUSE))
        val resumed = prepareTimerNotificationChange(paused,
            applyTimerNotificationAction(paused, 5, TimerNotificationAction.RESUME))
        assertTrue(resumed.timerRunning)
        assertEquals(resumed, applyTimerNotificationAction(resumed, 4, TimerNotificationAction.CANCEL))
    }
    @Test fun dismissSurvivesPauseResumeAndRebootCatchupButNewTimerResetsIt() {
        val hidden = prepareTimerNotificationChange(running,
            applyTimerNotificationAction(running, 4, TimerNotificationAction.DISMISS))
        val paused = prepareTimerNotificationChange(hidden, toggleCountdown(hidden))
        val resumed = prepareTimerNotificationChange(paused, toggleCountdown(paused))
        assertTrue(resumed.ongoingNotificationDismissed)
        assertTrue(advanceClockState(resumed.copy(savedAt = 1000), 2000, 100, 2).ongoingNotificationDismissed)
        val reset = prepareTimerNotificationChange(resumed, resetCountdown(resumed))
        assertFalse(prepareTimerNotificationChange(reset, toggleCountdown(reset)).ongoingNotificationDismissed)
    }
    @Test fun completionAndDisabledSettingsRejectControls() {
        val complete = running.copy(timerFinished = true, timerRunning = false, timerRemainingMillis = 0)
        assertEquals(complete, applyTimerNotificationAction(complete, 4, TimerNotificationAction.RESUME))
        val disabled = running.copy(ongoingNotificationEnabled = false)
        assertEquals(disabled, applyTimerNotificationAction(disabled, 4, TimerNotificationAction.CANCEL))
    }
    @Test fun cancelClearsCountdownWithoutChangingStopwatch() {
        val state = running.copy(stopwatchRunning = true, stopwatchElapsedMillis = 20)
        val cancelled = applyTimerNotificationAction(state, 4, TimerNotificationAction.CANCEL)
        assertEquals(0L, cancelled.timerRemainingMillis)
        assertFalse(cancelled.timerRunning)
        assertTrue(cancelled.stopwatchRunning)
    }
    @Test fun elapsedTicksDoNotInvalidateControls() {
        val advanced = advanceClockState(running.copy(savedAt = 1000, savedElapsed = 100, bootCount = 1), 2000, 1100, 1)
        assertEquals(4L, advanced.notificationRevision)
        assertFalse(applyTimerNotificationAction(advanced, 4, TimerNotificationAction.PAUSE).timerRunning)
    }
    @Test fun stopwatchAndCountdownRemainIndependent() {
        val both = running.copy(stopwatchRunning = true, stopwatchElapsedMillis = 10_000, stopwatchNotificationRevision = 8)
        val paused = prepareTimerNotificationChange(both, applyStopwatchNotificationAction(both, 8, TimerNotificationAction.PAUSE))
        assertTrue(paused.timerRunning)
        assertEquals(4L, paused.notificationRevision)
        assertFalse(paused.stopwatchRunning)
        assertEquals(paused, applyStopwatchNotificationAction(paused, 8, TimerNotificationAction.RESUME))
        val hidden = prepareTimerNotificationChange(both, applyStopwatchNotificationAction(both, 8, TimerNotificationAction.DISMISS))
        assertFalse(hidden.ongoingNotificationDismissed)
        assertTrue(hidden.stopwatchNotificationDismissed)
    }
    @Test fun newTimerRejectsOldCancel() {
        val reset = prepareTimerNotificationChange(running, resetCountdown(running))
        val newTimer = prepareTimerNotificationChange(reset, toggleCountdown(reset))
        assertEquals(newTimer, applyTimerNotificationAction(newTimer, 4, TimerNotificationAction.CANCEL))
    }
    @Test fun explicitlyRestoringTimerDoesNotRestoreStopwatch() {
        val hidden = running.copy(ongoingNotificationDismissed = true, stopwatchNotificationDismissed = true)
        val shown = prepareTimerNotificationChange(hidden, hidden.copy(ongoingNotificationDismissed = false))
        assertFalse(shown.ongoingNotificationDismissed)
        assertTrue(shown.stopwatchNotificationDismissed)
        assertEquals(hidden.stopwatchNotificationRevision, shown.stopwatchNotificationRevision)
    }
}
