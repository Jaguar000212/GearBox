package com.jaguar.gearbox

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.jaguar.gearbox.logic.ClockState
import com.jaguar.gearbox.logic.TimerNotificationAction
import com.jaguar.gearbox.logic.applyTimerNotificationAction
import com.jaguar.gearbox.logic.applyStopwatchNotificationAction
import com.jaguar.gearbox.logic.prepareTimerNotificationChange
import com.jaguar.gearbox.timer.ClockStateStorage
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

/** Uses test-package, uniquely named preferences. Never touches app data, alarms or notifications. */
class ClockNotificationPersistenceTest {
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val name = "clock_persistence_test_${UUID.randomUUID()}"
    private val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    private val storage = ClockStateStorage(prefs)

    @After fun cleanup() { context.deleteSharedPreferences(name) }

    private fun update(transform: (ClockState) -> ClockState) {
        val current = storage.load()
        storage.save(prepareTimerNotificationChange(current, transform(current)))
    }

    @Test fun sharedOptInAndIndependentDismissalSurviveReload() {
        update { it.copy(ongoingNotificationEnabled = true, timerRemainingMillis = 90_000,
            stopwatchElapsedMillis = 30_000, laps = listOf(20_000)) }
        val initial = storage.load()
        update { applyTimerNotificationAction(it, initial.notificationRevision, TimerNotificationAction.DISMISS) }
        val afterTimer = ClockStateStorage(prefs).load()
        assertTrue(afterTimer.ongoingNotificationEnabled)
        assertTrue(afterTimer.ongoingNotificationDismissed)
        assertFalse(afterTimer.stopwatchNotificationDismissed)
        update { applyStopwatchNotificationAction(it, afterTimer.stopwatchNotificationRevision, TimerNotificationAction.DISMISS) }
        val reloaded = ClockStateStorage(prefs).load()
        assertTrue(reloaded.ongoingNotificationDismissed)
        assertTrue(reloaded.stopwatchNotificationDismissed)
        assertEquals(listOf(20_000L), reloaded.laps)
    }

    @Test fun unrelatedSentinelSurvivesClockWritesAndOtherPreferenceFilesRemainUntouched() {
        prefs.edit().putString("unrelated.sentinel", "keep me").commit()
        val untouchedName = "${name}_untouched"
        val untouched = context.getSharedPreferences(untouchedName, Context.MODE_PRIVATE)
        try {
            untouched.edit().putString("settings.sentinel", "preserve")
                .putLong("timer_stopwatch.sw_elapsed", 123456L).commit()
            val before = untouched.all.toMap()
            update { it.copy(ongoingNotificationEnabled = true, stopwatchElapsedMillis = 5000) }
            update { ClockState() }
            assertEquals("keep me", prefs.getString("unrelated.sentinel", null))
            context.deleteSharedPreferences(name)
            assertEquals(before, untouched.all)
        } finally { context.deleteSharedPreferences(untouchedName) }
    }
}
