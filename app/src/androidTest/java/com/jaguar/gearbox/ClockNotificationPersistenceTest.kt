package com.jaguar.gearbox

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.jaguar.gearbox.logic.ClockState
import com.jaguar.gearbox.logic.TimerNotificationAction
import com.jaguar.gearbox.logic.applyTimerNotificationAction
import com.jaguar.gearbox.logic.applyStopwatchNotificationAction
import com.jaguar.gearbox.timer.ClockRepository
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ClockNotificationPersistenceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    @Before fun setup() { clear() }
    @After fun clear() {
        ClockRepository.update(context) { ClockState() }
        context.getSharedPreferences("gearbox_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }
    @Test fun sharedOptInAndIndependentDismissalSurviveReload() {
        val initial = ClockRepository.update(context) {
            it.copy(ongoingNotificationEnabled = true, timerRemainingMillis = 90_000,
                stopwatchElapsedMillis = 30_000, laps = listOf(20_000))
        }
        ClockRepository.update(context) {
            applyTimerNotificationAction(it, initial.notificationRevision, TimerNotificationAction.DISMISS)
        }
        val afterTimer = ClockRepository.snapshot(context)
        assertTrue(afterTimer.ongoingNotificationEnabled)
        assertTrue(afterTimer.ongoingNotificationDismissed)
        assertFalse(afterTimer.stopwatchNotificationDismissed)
        ClockRepository.update(context) {
            applyStopwatchNotificationAction(it, afterTimer.stopwatchNotificationRevision, TimerNotificationAction.DISMISS)
        }
        ClockRepository.restoreAlarm(context)
        val reloaded = ClockRepository.snapshot(context)
        assertTrue(reloaded.ongoingNotificationDismissed)
        assertTrue(reloaded.stopwatchNotificationDismissed)
        assertEquals(listOf(20_000L), reloaded.laps)
    }
}
