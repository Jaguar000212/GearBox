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

    private fun storage(context: Context) = ClockStateStorage(context.applicationContext
        .getSharedPreferences("gearbox_prefs", Context.MODE_PRIVATE))

    private fun load(context: Context) = storage(context).load()
    private fun save(context: Context, state: ClockState) = storage(context).save(state)

    private fun advance(context: Context, state: ClockState) = advanceClockState(
        state, System.currentTimeMillis(), SystemClock.elapsedRealtime(),
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1),
    )

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
