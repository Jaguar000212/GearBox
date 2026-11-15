package com.jaguar.gearbox.timer

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import com.jaguar.gearbox.logic.ClockState
import com.jaguar.gearbox.logic.advanceClockState
import com.jaguar.gearbox.logic.claimTimerCompletion
import com.jaguar.gearbox.logic.prepareTimerNotificationChange

/** One process-wide owner prevents stale screen snapshots from overwriting widget/alarm actions. */
object ClockRepository {
    private val lock = Any()
    private const val PREFIX = "timer_stopwatch."

    private fun prefs(context: Context) = context.applicationContext
        .getSharedPreferences("gearbox_prefs", Context.MODE_PRIVATE)

    private fun load(context: Context): ClockState {
        val p = prefs(context)
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

    private fun advance(context: Context, state: ClockState) = advanceClockState(
        state, System.currentTimeMillis(), SystemClock.elapsedRealtime(),
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1),
    )

    private fun save(context: Context, s: ClockState) {
        // Commit before scheduling: a reboot must not lose a newly started timer's state.
        check(prefs(context).edit()
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

    fun snapshot(context: Context): ClockState = synchronized(lock) { advance(context, load(context)) }

    /** The caller transforms fresh state, not a Compose or widget snapshot. */
    fun update(context: Context, transform: (ClockState) -> ClockState): ClockState = synchronized(lock) {
        val current = refresh(context)
        val next = prepareTimerNotificationChange(current, transform(current))
        save(context, next)
        if (!next.timerFinished) TimerAlerts.dismissCompletion(context)
        TimerAlerts.schedule(context, next)
        TimerStatusNotification.sync(context, next)
        StopwatchStatusNotification.sync(context, next)
        next
    }

    /** Both foreground refresh and OS alarms use this idempotent completion path. */
    fun refresh(context: Context, expectedGeneration: Long? = null): ClockState = synchronized(lock) {
        val current = advance(context, load(context))
        val claimed = claimTimerCompletion(current, expectedGeneration)
        if (claimed != null) {
            save(context, claimed)
            TimerAlerts.cancel(context)
            TimerStatusNotification.cancel(context)
            TimerAlerts.notifyCompletion(context)
            claimed
        } else current
    }

    fun restoreAlarm(context: Context) = synchronized(lock) {
        val state = refresh(context)
        if (state.timerRunning) {
            save(context, state)
            TimerAlerts.schedule(context, state)
        }
        TimerStatusNotification.sync(context, state)
        StopwatchStatusNotification.sync(context, state)
    }
}
