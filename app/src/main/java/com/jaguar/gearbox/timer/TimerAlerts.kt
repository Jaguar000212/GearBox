package com.jaguar.gearbox.timer

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.updateAll
import com.jaguar.gearbox.MainActivity
import com.jaguar.gearbox.data.EXTRA_SHORTCUT_ROUTE
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.logic.ClockState
import com.jaguar.gearbox.widget.TimerWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object TimerAlerts {
    private const val CHANNEL = "timer_completion"
    private const val NOTIFICATION_ID = 4101
    const val ACTION_COMPLETE = "com.jaguar.gearbox.TIMER_COMPLETE"
    const val EXTRA_GENERATION = "timer_generation"
    @Volatile var schedulingFailed: Boolean = false
        private set

    fun exactAlarmsAllowed(context: Context): Boolean = Build.VERSION.SDK_INT < 31 ||
        context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true

    fun notificationsAllowed(context: Context): Boolean {
        ensureChannel(context)
        val runtimeAllowed = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        val channel = manager.getNotificationChannel(CHANNEL)
        return runtimeAllowed && NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            channel != null && channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    private fun alarmIntent(context: Context, generation: Long = 0): PendingIntent = requireNotNull(PendingIntent.getBroadcast(
        context, 4101,
        Intent(context, TimerAlarmReceiver::class.java).setAction(ACTION_COMPLETE)
            .putExtra(EXTRA_GENERATION, generation),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    ))

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java)?.cancel(alarmIntent(context))
    }

    fun dismissCompletion(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    @SuppressLint("ScheduleExactAlarm")
    fun schedule(context: Context, state: ClockState) {
        if (!state.timerRunning || state.timerRemainingMillis <= 0) {
            cancel(context)
            return
        }
        val manager = context.getSystemService(AlarmManager::class.java) ?: run {
            schedulingFailed = true
            return
        }
        val intent = alarmIntent(context, state.generation)
        val now = SystemClock.elapsedRealtime()
        val base = state.savedElapsed.takeIf { it >= 0 } ?: now
        val trigger = (base + state.timerRemainingMillis.coerceAtMost(Long.MAX_VALUE - base)).coerceAtLeast(now)
        schedulingFailed = false
        try {
            if (exactAlarmsAllowed(context)) {
                manager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, intent)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, intent)
            }
        } catch (_: SecurityException) {
            // Permission can change between checking and scheduling. Keep a best-effort alarm.
            runCatching { manager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, intent) }
                .onFailure { schedulingFailed = true }
        } catch (_: RuntimeException) {
            schedulingFailed = true
        }
    }

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL) != null) return
        val channel = NotificationChannel(CHANNEL, "Timer completion", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "One notification when a countdown finishes"
            enableVibration(true)
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build(),
            )
        }
        manager.createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    fun notifyCompletion(context: Context) {
        if (!notificationsAllowed(context)) return
        val openTimer = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_SHORTCUT_ROUTE, Tools.ROUTE_TIMER_STOPWATCH)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val contentIntent = PendingIntent.getActivity(
            context, 4101, openTimer, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Time's up!")
            .setContentText("Your GearBox timer has finished.")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        // A permission may be revoked after the check. Do not crash the broadcast receiver.
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
    }
}

class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TimerAlerts.ACTION_COMPLETE || !intent.hasExtra(TimerAlerts.EXTRA_GENERATION)) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                runCatching {
                    ClockRepository.refresh(context, intent.getLongExtra(TimerAlerts.EXTRA_GENERATION, -1))
                    TimerWidget().updateAll(context)
                }
            } finally { pending.finish() }
        }
    }
}

/** Re-register after reboot/update or after the user grants exact-alarm access. */
class TimerRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(
                Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
                AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            )) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                runCatching {
                    ClockRepository.restoreAlarm(context)
                    TimerWidget().updateAll(context)
                }
            } finally { pending.finish() }
        }
    }
}
