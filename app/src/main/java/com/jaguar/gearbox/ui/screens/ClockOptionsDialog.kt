package com.jaguar.gearbox.ui.screens

import com.jaguar.gearbox.R
import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.jaguar.gearbox.logic.ClockState
import com.jaguar.gearbox.timer.*
import com.jaguar.gearbox.widget.TimerWidget
import kotlinx.coroutines.*

/** Both entry points edit the same repository, never a duplicate notification preference. */
@Composable
fun ClockOptionsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(ClockRepository.snapshot(context)) }
    var notificationsAllowed by remember { mutableStateOf(TimerAlerts.notificationsAllowed(context)) }
    var exactAllowed by remember { mutableStateOf(TimerAlerts.exactAlarmsAllowed(context)) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsAllowed = TimerAlerts.notificationsAllowed(context)
        scope.launch {
            try { withContext(Dispatchers.IO) { ClockRepository.restoreAlarm(context) } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { error = context.getString(R.string.screen_could_not_refresh_clock_alerts_please_try_again) }
        }
    }
    fun change(transform: (ClockState) -> ClockState) {
        if (saving) return
        saving = true
        scope.launch {
            try {
                state = withContext(Dispatchers.IO) {
                    val next = ClockRepository.update(context, transform)
                    TimerWidget().updateAll(context)
                    next
                }
                error = null
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { error = context.getString(R.string.screen_could_not_save_clock_options_please_try_again) }
            finally { saving = false }
        }
    }
    fun openAndroid(intent: Intent) {
        runCatching { context.startActivity(intent) }
            .onFailure { error = context.getString(R.string.screen_open_gearbox_s_notification_or_alarm_permissions_in_android_setti) }
    }
    LaunchedEffect(lifecycle, context) {
        lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                try { state = withContext(Dispatchers.IO) { ClockRepository.snapshot(context) } }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { error = context.getString(R.string.screen_could_not_read_clock_options_please_try_again) }
                notificationsAllowed = TimerAlerts.notificationsAllowed(context)
                exactAllowed = TimerAlerts.exactAlarmsAllowed(context)
                delay(500)
            }
        }
    }
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(context.getString(R.string.screen_clock_options)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(context.getString(R.string.screen_these_preferences_apply_to_both_timer_and_stopwatch_changing_them))
                ListItem(
                    headlineContent = { Text(context.getString(R.string.screen_ongoing_clock_notifications)) },
                    supportingContent = { Text(context.getString(R.string.screen_show_active_clocks_outside_the_app_eligible_notifications_may_app)) },
                    trailingContent = {
                        Switch(checked = state.ongoingNotificationEnabled, enabled = !saving, onCheckedChange = { enabled ->
                            change { it.copy(ongoingNotificationEnabled = enabled,
                                ongoingNotificationDismissed = false, stopwatchNotificationDismissed = false) }
                            if (enabled && Build.VERSION.SDK_INT >= 33 && !notificationsAllowed)
                                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        })
                    },
                )
                if (state.ongoingNotificationEnabled && !TimerStatusNotification.allowed(context))
                    Text(context.getString(R.string.screen_ongoing_notifications_are_blocked_by_android_settings), color = MaterialTheme.colorScheme.error)
                if (state.ongoingNotificationEnabled && state.stopwatchNotificationDismissed) {
                    TextButton(enabled = !saving, onClick = { change { it.copy(stopwatchNotificationDismissed = false) } }) {
                        Text(context.getString(R.string.screen_show_stopwatch_notification_again))
                    }
                }
                if (state.ongoingNotificationEnabled && state.ongoingNotificationDismissed) {
                    TextButton(enabled = !saving, onClick = { change { it.copy(ongoingNotificationDismissed = false) } }) {
                        Text(context.getString(R.string.screen_show_timer_notification_again))
                    }
                }
                HorizontalDivider()
                Text(context.getString(R.string.screen_completion_alerts), style = MaterialTheme.typography.titleSmall)
                Text(context.getString(R.string.screen_completion_alerts_are_separate_from_ongoing_notifications_android))
                if (!notificationsAllowed && Build.VERSION.SDK_INT >= 33) {
                    TextButton(onClick = { permission.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text(context.getString(R.string.screen_allow_notifications)) }
                }
                TextButton(onClick = { openAndroid(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }) { Text(context.getString(R.string.screen_android_notification_settings)) }
                if (!exactAllowed && Build.VERSION.SDK_INT >= 31) {
                    Text(context.getString(R.string.screen_without_precise_alarm_access_a_best_effort_completion_alert_may_b))
                    TextButton(onClick = { openAndroid(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        "package:${context.packageName}".toUri())) }) { Text(context.getString(R.string.screen_allow_precise_alarms)) }
                }
                if (TimerAlerts.schedulingFailed) Text(context.getString(R.string.screen_the_alert_could_not_be_scheduled_check_android_settings_and_resta), color = MaterialTheme.colorScheme.error)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text(context.getString(R.string.screen_done)) } },
    )
}
