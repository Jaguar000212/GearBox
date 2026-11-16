package com.jaguar.gearbox.timer

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.glance.appwidget.updateAll
import com.jaguar.gearbox.MainActivity
import com.jaguar.gearbox.data.EXTRA_SHORTCUT_ROUTE
import com.jaguar.gearbox.logic.ClockState
import com.jaguar.gearbox.logic.TimerNotificationAction
import com.jaguar.gearbox.logic.applyStopwatchNotificationAction
import com.jaguar.gearbox.widget.TimerWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

/** Separate identity from countdown: both may run at once. Uses the same explicit opt-in. */
object StopwatchStatusNotification {
    const val STOPWATCH_ROUTE = "stopwatch_notification"
    const val RESET_ROUTE = "stopwatch_notification_reset"
    private const val ID = 4103
    private const val PREFIX = "com.jaguar.gearbox.stopwatch.status."
    private const val REVISION = "stopwatch_notification_revision"

    private fun action(context: Context, revision: Long, action: TimerNotificationAction) = PendingIntent.getBroadcast(
        context, ID, Intent(context, StopwatchStatusActionReceiver::class.java)
            .setAction(PREFIX + action.name).setData(Uri.parse("gearbox://stopwatch-status/$revision/${action.name}"))
            .putExtra(REVISION, revision), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun open(context: Context, reset: Boolean) = PendingIntent.getActivity(context, if (reset) 4104 else ID,
        Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_SHORTCUT_ROUTE, if (reset) RESET_ROUTE else STOPWATCH_ROUTE)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    @SuppressLint("MissingPermission")
    fun sync(context: Context, state: ClockState) {
        val manager = NotificationManagerCompat.from(context)
        if (!state.ongoingNotificationEnabled || state.stopwatchNotificationDismissed ||
            (!state.stopwatchRunning && state.stopwatchElapsedMillis <= 0) || !TimerStatusNotification.allowed(context)) {
            manager.cancel(ID)
            return
        }
        val gap = if (state.stopwatchRunning && state.savedElapsed >= 0)
            (SystemClock.elapsedRealtime() - state.savedElapsed).coerceAtLeast(0) else 0L
        val elapsed = state.stopwatchElapsedMillis + gap.coerceAtMost(Long.MAX_VALUE - state.stopwatchElapsedMillis)
        val seconds = elapsed / 1000
        val builder = NotificationCompat.Builder(context, TimerStatusNotification.CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(if (state.stopwatchRunning) context.getString(com.jaguar.gearbox.R.string.system_stopwatch_running) else context.getString(com.jaguar.gearbox.R.string.system_stopwatch_paused))
            .setContentText(if (state.stopwatchRunning) context.getString(com.jaguar.gearbox.R.string.system_elapsed_time) else context.getString(com.jaguar.gearbox.R.string.system_stopwatch_elapsed, seconds / 3600, seconds / 60 % 60, seconds % 60))
            .setCategory(NotificationCompat.CATEGORY_PROGRESS).setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true).setOngoing(state.stopwatchRunning)
            .setContentIntent(open(context, false))
            .setDeleteIntent(action(context, state.stopwatchNotificationRevision, TimerNotificationAction.DISMISS))
            .addAction(0, if (state.stopwatchRunning) context.getString(com.jaguar.gearbox.R.string.system_pause) else context.getString(com.jaguar.gearbox.R.string.system_resume),
                action(context, state.stopwatchNotificationRevision, if (state.stopwatchRunning) TimerNotificationAction.PAUSE else TimerNotificationAction.RESUME))
            // Reset opens an in-app confirmation rather than silently discarding elapsed time/laps.
            .addAction(0, context.getString(com.jaguar.gearbox.R.string.system_reset_ellipsis), open(context, true))
        if (state.stopwatchRunning) {
            builder.setWhen(System.currentTimeMillis() - elapsed).setUsesChronometer(true).setShowWhen(true)
            if (Build.VERSION.SDK_INT >= 36) builder.addExtras(Bundle().apply { putBoolean("android.requestPromotedOngoing", true) })
        } else builder.setShowWhen(false)
        runCatching { manager.notify(ID, builder.build()) }
    }

    internal fun parseAction(intent: Intent) = TimerNotificationAction.entries.firstOrNull { intent.action == PREFIX + it.name }
    internal fun revision(intent: Intent) = intent.getLongExtra(REVISION, -1)
}

class StopwatchStatusActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = StopwatchStatusNotification.parseAction(intent) ?: return
        // Reset is intentionally an Activity confirmation, never a destructive broadcast.
        if (action == TimerNotificationAction.CANCEL) return
        val revision = StopwatchStatusNotification.revision(intent)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                runCatching {
                    ClockRepository.update(context) { applyStopwatchNotificationAction(it, revision, action) }
                    TimerWidget().updateAll(context)
                }
            } finally { pending.finish() }
        }
    }
}
