package com.jaguar.gearbox.ui.screens

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
            catch (_: Exception) { error = "Could not refresh clock alerts. Please try again." }
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
            catch (_: Exception) { error = "Could not save clock options. Please try again." }
            finally { saving = false }
        }
    }
    fun openAndroid(intent: Intent) {
        runCatching { context.startActivity(intent) }
            .onFailure { error = "Open GearBox’s notification or alarm permissions in Android Settings." }
    }
    LaunchedEffect(lifecycle, context) {
        lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                try { state = withContext(Dispatchers.IO) { ClockRepository.snapshot(context) } }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { error = "Could not read clock options. Please try again." }
                notificationsAllowed = TimerAlerts.notificationsAllowed(context)
                exactAllowed = TimerAlerts.exactAlarmsAllowed(context)
                delay(500)
            }
        }
    }
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Clock options") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("These preferences apply to both Timer and Stopwatch. Changing them does not stop a running clock.")
                ListItem(
                    headlineContent = { Text("Ongoing clock notifications") },
                    supportingContent = { Text("Show active clocks outside the app. Eligible notifications may appear as Live Updates depending on Android and device settings.") },
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
                    Text("Ongoing notifications are blocked by Android settings.", color = MaterialTheme.colorScheme.error)
                if (state.ongoingNotificationEnabled && state.stopwatchNotificationDismissed) {
                    TextButton(enabled = !saving, onClick = { change { it.copy(stopwatchNotificationDismissed = false) } }) {
                        Text("Show stopwatch notification again")
                    }
                }
                if (state.ongoingNotificationEnabled && state.ongoingNotificationDismissed) {
                    TextButton(enabled = !saving, onClick = { change { it.copy(ongoingNotificationDismissed = false) } }) {
                        Text("Show timer notification again")
                    }
                }
                HorizontalDivider()
                Text("Completion alerts", style = MaterialTheme.typography.titleSmall)
                Text("Completion alerts are separate from ongoing notifications. Android battery restrictions, Do Not Disturb and force-stop can delay or prevent alerts. Active timers are restored after reboot and unlock.")
                if (!notificationsAllowed && Build.VERSION.SDK_INT >= 33) {
                    TextButton(onClick = { permission.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("Allow notifications") }
                }
                TextButton(onClick = { openAndroid(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }) { Text("Android notification settings") }
                if (!exactAllowed && Build.VERSION.SDK_INT >= 31) {
                    Text("Without precise alarm access, a best-effort completion alert may be late.")
                    TextButton(onClick = { openAndroid(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        "package:${context.packageName}".toUri())) }) { Text("Allow precise alarms") }
                }
                if (TimerAlerts.schedulingFailed) Text("The alert could not be scheduled. Check Android settings and restart the timer.", color = MaterialTheme.colorScheme.error)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Done") } },
    )
}
