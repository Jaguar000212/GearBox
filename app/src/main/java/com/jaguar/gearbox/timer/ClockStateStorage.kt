package com.jaguar.gearbox.timer

import android.content.SharedPreferences
import com.jaguar.gearbox.logic.ClockState

/** Persistence only: callers own timing and notification/alarm effects. */
internal class ClockStateStorage(private val preferences: SharedPreferences) {
    private companion object { const val PREFIX = "timer_stopwatch." }

    fun load(): ClockState {
        val p = preferences
        val running = p.getBoolean(PREFIX + "timer_running", false)
        return ClockState(
            mode = p.getString(PREFIX + "mode", "STOPWATCH") ?: "STOPWATCH",
            stopwatchElapsedMillis = p.getLong(PREFIX + "sw_elapsed", 0),
            stopwatchRunning = p.getBoolean(PREFIX + "sw_running", false),
            laps = p.getString(PREFIX + "laps", "").orEmpty().split(',').mapNotNull { it.toLongOrNull() },
            timerRemainingMillis = p.getLong(PREFIX + "timer_remaining", 0),
            timerRunning = running,
            timerFinished = p.getBoolean(PREFIX + "timer_finished", false),
            minutesInput = p.getString(PREFIX + "minutes_input", "5") ?: "5",
            secondsInput = p.getString(PREFIX + "seconds_input", "0") ?: "0",
            savedAt = p.getLong(PREFIX + "saved_at", 0),
            savedElapsed = p.getLong(PREFIX + "saved_elapsed", -1),
            bootCount = p.getInt(PREFIX + "boot_count", -1),
            generation = p.getLong(PREFIX + "generation", if (running) 1 else 0),
            alertedGeneration = p.getLong(PREFIX + "alerted_generation", 0),
            ongoingNotificationEnabled = p.getBoolean(PREFIX + "ongoing_enabled", false),
            ongoingNotificationDismissed = p.getBoolean(PREFIX + "ongoing_dismissed", false),
            notificationRevision = p.getLong(PREFIX + "notification_revision", 0),
            stopwatchNotificationDismissed = p.getBoolean(PREFIX + "sw_notification_dismissed", false),
            stopwatchNotificationRevision = p.getLong(PREFIX + "sw_notification_revision", 0),
        )
    }

    fun save(s: ClockState) {
        // Commit before scheduling: a reboot must not lose a newly started timer's state.
        check(preferences.edit()
            .putString(PREFIX + "mode", s.mode)
            .putLong(PREFIX + "sw_elapsed", s.stopwatchElapsedMillis)
            .putBoolean(PREFIX + "sw_running", s.stopwatchRunning)
            .putString(PREFIX + "laps", s.laps.joinToString(","))
            .putLong(PREFIX + "timer_remaining", s.timerRemainingMillis)
            .putBoolean(PREFIX + "timer_running", s.timerRunning)
            .putBoolean(PREFIX + "timer_finished", s.timerFinished)
            .putString(PREFIX + "minutes_input", s.minutesInput)
            .putString(PREFIX + "seconds_input", s.secondsInput)
            .putLong(PREFIX + "saved_at", s.savedAt)
            .putLong(PREFIX + "saved_elapsed", s.savedElapsed)
            .putInt(PREFIX + "boot_count", s.bootCount)
            .putLong(PREFIX + "generation", s.generation)
            .putLong(PREFIX + "alerted_generation", s.alertedGeneration)
            .putBoolean(PREFIX + "ongoing_enabled", s.ongoingNotificationEnabled)
            .putBoolean(PREFIX + "ongoing_dismissed", s.ongoingNotificationDismissed)
            .putLong(PREFIX + "notification_revision", s.notificationRevision)
            .putBoolean(PREFIX + "sw_notification_dismissed", s.stopwatchNotificationDismissed)
            .putLong(PREFIX + "sw_notification_revision", s.stopwatchNotificationRevision)
            .commit()) { "Could not save timer state" }
    }

}
