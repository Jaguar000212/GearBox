package com.jaguar.gearbox.focus

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
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.jaguar.gearbox.MainActivity
import com.jaguar.gearbox.R
import com.jaguar.gearbox.data.EXTRA_SHORTCUT_ROUTE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object FocusAlerts {
    private const val CHANNEL = "focus_phase_changes"
    private const val ID = 4301
    const val ACTION_BOUNDARY = "com.jaguar.gearbox.FOCUS_BOUNDARY"
    const val EXTRA_GENERATION = "focus_generation"
    @Volatile var schedulingFailed: Boolean = false
        private set

    fun exactAlarmsAllowed(context: Context): Boolean = Build.VERSION.SDK_INT < 31 ||
        context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true
    fun notificationsAllowed(context: Context): Boolean {
        ensureChannel(context)
        val permitted = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context,
            Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return permitted && NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            context.getSystemService(NotificationManager::class.java)?.getNotificationChannel(CHANNEL)?.importance
                ?.let { it != NotificationManager.IMPORTANCE_NONE } == true
    }
    private fun ensureChannel(context: Context) {
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.focus_backend_channel), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.focus_backend_channel_description)
                enableVibration(true)
            })
    }
    private fun alarm(context: Context, generation: Long = 0): PendingIntent = requireNotNull(PendingIntent.getBroadcast(
        context, ID, Intent(context, FocusAlarmReceiver::class.java).setAction(ACTION_BOUNDARY)
            .putExtra(EXTRA_GENERATION, generation), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
    fun dismiss(context: Context) { runCatching { NotificationManagerCompat.from(context).cancel(ID) } }

    @SuppressLint("ScheduleExactAlarm")
    fun schedule(context: Context, state: FocusState) {
        val manager = context.getSystemService(AlarmManager::class.java)
        schedulingFailed = false
        if (manager == null) { schedulingFailed = state.status == FocusStatus.RUNNING; return }
        val pending = alarm(context, state.generation)
        if (state.status != FocusStatus.RUNNING) {
            runCatching { manager.cancel(pending) }.onFailure { schedulingFailed = true }; return
        }
        val trigger = (state.savedElapsed + state.remainingMillis.coerceAtMost(Long.MAX_VALUE - state.savedElapsed)).coerceAtLeast(SystemClock.elapsedRealtime())
        try {
            if (exactAlarmsAllowed(context)) manager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pending)
            else manager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pending)
        } catch (_: SecurityException) {
            runCatching { manager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pending) }
                .onFailure { schedulingFailed = true }
        } catch (_: RuntimeException) { schedulingFailed = true }
    }

    @SuppressLint("MissingPermission")
    fun notifyBoundary(context: Context, state: FocusState) {
        if (!notificationsAllowed(context)) return
        val finished = state.status == FocusStatus.FINISHED
        val phase = context.getString(when (state.phase.kind) {
            FocusPhaseKind.WORK -> R.string.focus_backend_work
            FocusPhaseKind.SHORT_BREAK -> R.string.focus_backend_short_break
            FocusPhaseKind.LONG_BREAK -> R.string.focus_backend_long_break
        })
        val open = PendingIntent.getActivity(context, ID,
            Intent(context, MainActivity::class.java).putExtra(EXTRA_SHORTCUT_ROUTE, "tool/focus_timer")
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(if (finished) R.string.focus_backend_finished else R.string.focus_backend_phase_title))
            .setContentText(if (finished) context.getString(R.string.focus_backend_finished_body)
                else context.getString(R.string.focus_backend_phase_body, phase, state.phase.round, state.config.rounds))
            .setCategory(NotificationCompat.CATEGORY_ALARM).setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open).setAutoCancel(true).build()
        runCatching { NotificationManagerCompat.from(context).notify(ID, notification) }
    }
}

class FocusAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != FocusAlerts.ACTION_BOUNDARY || !intent.hasExtra(FocusAlerts.EXTRA_GENERATION)) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { runCatching { FocusRepository(context).refresh(intent.getLongExtra(FocusAlerts.EXTRA_GENERATION, -1), reschedule = true) } }
            finally { pending.finish() }
        }
    }
}

/** Reboot interrupts rather than resumes; package/permission changes re-register same-boot state. */
class FocusRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
                AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { runCatching { FocusRepository(context).read() } }
            finally { pending.finish() }
        }
    }
}
