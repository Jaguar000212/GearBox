package com.jaguar.gearbox.timer

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.updateAll
import com.jaguar.gearbox.MainActivity
import com.jaguar.gearbox.R
import com.jaguar.gearbox.data.EXTRA_SHORTCUT_ROUTE
import com.jaguar.gearbox.logic.ClockState
import com.jaguar.gearbox.logic.TimerNotificationAction
import com.jaguar.gearbox.logic.applyTimerNotificationAction
import com.jaguar.gearbox.widget.TimerWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

/** Presentation only. No service, ticking worker, or second clock owner. */
object TimerStatusNotification {
    const val COUNTDOWN_ROUTE = "timer_countdown_notification"
    internal const val CHANNEL = "timer_status"
    internal const val ID = 4102
    internal const val EXTRA_REVISION = "notification_revision"
    private const val ACTION_PREFIX = "com.jaguar.gearbox.timer.status."

    fun allowed(context: Context): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        if (manager.getNotificationChannel(CHANNEL) == null) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL,
                context.getString(R.string.timer_status_channel), NotificationManager.IMPORTANCE_LOW).apply {
                description = context.getString(R.string.timer_status_channel_description)
                setSound(null, null)
                enableVibration(false)
            })
        }
        return (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context,
            Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            manager.getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun cancel(context: Context) { NotificationManagerCompat.from(context).cancel(ID) }

    private fun action(context: Context, revision: Long, action: TimerNotificationAction): PendingIntent =
        requireNotNull(PendingIntent.getBroadcast(context, ID,
            Intent(context, TimerStatusActionReceiver::class.java)
                .setAction(ACTION_PREFIX + action.name)
                // Extras are not identity: retain the original revision in already delivered actions.
                .setData(Uri.parse("gearbox://timer-status/$revision/${action.name}"))
                .putExtra(EXTRA_REVISION, revision),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

    internal fun parseAction(intent: Intent): TimerNotificationAction? =
        TimerNotificationAction.entries.firstOrNull { intent.action == ACTION_PREFIX + it.name }

    @SuppressLint("MissingPermission")
    fun sync(context: Context, state: ClockState) {
        if (!state.ongoingNotificationEnabled || state.ongoingNotificationDismissed ||
            state.timerFinished || state.timerRemainingMillis <= 0 || !allowed(context)) {
            cancel(context)
            return
        }
        // Account for persistence/notification-building time without changing the clock owner.
        val remaining = if (state.timerRunning && state.savedElapsed >= 0)
            (state.timerRemainingMillis - (SystemClock.elapsedRealtime() - state.savedElapsed).coerceAtLeast(0)).coerceAtLeast(0)
        else state.timerRemainingMillis
        if (remaining <= 0) { cancel(context); return }
        val open = PendingIntent.getActivity(context, ID,
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_SHORTCUT_ROUTE, COUNTDOWN_ROUTE)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(if (state.timerRunning) R.string.timer_status_running else R.string.timer_status_paused))
            .setContentText(if (state.timerRunning) context.getString(R.string.timer_status_countdown)
                else context.getString(R.string.timer_status_remaining, formatRemaining(remaining)))
            .setContentIntent(open)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setOngoing(state.timerRunning)
            .setDeleteIntent(action(context, state.notificationRevision, TimerNotificationAction.DISMISS))
            .addAction(0, context.getString(if (state.timerRunning) R.string.timer_status_pause else R.string.timer_status_resume),
                action(context, state.notificationRevision, if (state.timerRunning) TimerNotificationAction.PAUSE else TimerNotificationAction.RESUME))
            .addAction(0, context.getString(R.string.timer_status_cancel), action(context, state.notificationRevision, TimerNotificationAction.CANCEL))
        if (state.timerRunning) {
            builder.setWhen(System.currentTimeMillis() + remaining)
                .setShowWhen(true).setUsesChronometer(true).setChronometerCountDown(true)
                // Hide an expired countdown even if Android delays the completion alarm.
                .setTimeoutAfter(remaining)
            if (Build.VERSION.SDK_INT >= 36) {
                // Public API extra (36.1+). Unsupported versions/OEMs retain a normal notification.
                // Avoid raising compile/target SDK solely for optional presentation.
                builder.addExtras(Bundle().apply { putBoolean("android.requestPromotedOngoing", true) })
            }
        } else builder.setShowWhen(false)
        // Revocation races and device-specific notification failures must not break timer controls.
        runCatching { NotificationManagerCompat.from(context).notify(ID, builder.build()) }
    }

    internal fun formatRemaining(millis: Long): String {
        val seconds = (millis.coerceAtLeast(0) + 999) / 1000
        return String.format(Locale.getDefault(), "%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)
    }
}

class TimerStatusActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = TimerStatusNotification.parseAction(intent) ?: return
        if (!intent.hasExtra(TimerStatusNotification.EXTRA_REVISION)) return
        val revision = intent.getLongExtra(TimerStatusNotification.EXTRA_REVISION, -1)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                runCatching {
                    ClockRepository.update(context) { applyTimerNotificationAction(it, revision, action) }
                    TimerWidget().updateAll(context)
                }
            } finally { pending.finish() }
        }
    }
}
