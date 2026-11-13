package com.jaguar.gearbox.ui.tools

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.logic.ClockState
import com.jaguar.gearbox.logic.timerDurationMillis
import com.jaguar.gearbox.logic.toggleCountdown
import com.jaguar.gearbox.logic.resetCountdown
import com.jaguar.gearbox.timer.ClockRepository
import com.jaguar.gearbox.timer.TimerAlerts
import com.jaguar.gearbox.ui.components.ToolScaffold
import com.jaguar.gearbox.widget.TimerWidget
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Locale

private enum class ClockMode(val label: String) { STOPWATCH("Stopwatch"), TIMER("Timer") }

@Composable
fun TimerStopwatchScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val mutations = remember { Mutex() }
    var state by remember { mutableStateOf(ClockRepository.snapshot(context)) }
    var draftMinutes by rememberSaveable { mutableStateOf(state.minutesInput) }
    var draftSeconds by rememberSaveable { mutableStateOf(state.secondsInput) }
    var error by remember { mutableStateOf<String?>(null) }
    var notificationsAllowed by remember { mutableStateOf(TimerAlerts.notificationsAllowed(context)) }
    var exactAllowed by remember { mutableStateOf(TimerAlerts.exactAlarmsAllowed(context)) }
    val selectedMode = ClockMode.entries.firstOrNull { it.name == state.mode } ?: ClockMode.STOPWATCH
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsAllowed = TimerAlerts.notificationsAllowed(context)
    }

    fun change(updateWidgets: Boolean = true, transform: (ClockState) -> ClockState) {
        scope.launch {
            try {
                state = mutations.withLock {
                    withContext(Dispatchers.IO) {
                        val updated = ClockRepository.update(context, transform)
                        if (updateWidgets) TimerWidget().updateAll(context)
                        updated
                    }
                }
                error = null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                error = "Could not save the clock state. Please try again."
            }
        }
    }

    // The store is the only clock writer. Local text drafts update synchronously for fast typing;
    // polling cannot overwrite those drafts, and no disposal callback rewrites clock snapshots.
    LaunchedEffect(context, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            try {
                withContext(Dispatchers.IO) { ClockRepository.restoreAlarm(context) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                error = "Could not restore the timer alert. Please restart the timer."
            }
            while (true) {
                try {
                    val fresh = withContext(Dispatchers.IO) { ClockRepository.refresh(context) }
                    val justFinished = state.timerRunning && fresh.timerFinished
                    state = fresh
                    if (justFinished) withContext(Dispatchers.IO) { TimerWidget().updateAll(context) }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    error = "Could not update the clock state."
                }
                notificationsAllowed = TimerAlerts.notificationsAllowed(context)
                exactAllowed = TimerAlerts.exactAlarmsAllowed(context)
                delay(100)
            }
        }
    }


    ToolScaffold(
        title = "Timer / Stopwatch",
        icon = Tools.byRoute(Tools.ROUTE_TIMER_STOPWATCH)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        TabRow(selectedTabIndex = ClockMode.entries.indexOf(selectedMode)) {
            ClockMode.entries.forEach { entry ->
                Tab(selected = entry == selectedMode, onClick = { change { it.copy(mode = entry.name) } },
                    text = { Text(entry.label) })
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(20.dp))
        when (selectedMode) {
            ClockMode.STOPWATCH -> StopwatchSection(
                elapsedMillis = state.stopwatchElapsedMillis,
                isRunning = state.stopwatchRunning,
                laps = state.laps,
                onToggleRunning = { change { it.copy(stopwatchRunning = !it.stopwatchRunning) } },
                onLap = { change { if (it.stopwatchRunning) it.copy(laps = it.laps + it.stopwatchElapsedMillis) else it } },
                onReset = { change { it.copy(stopwatchRunning = false, stopwatchElapsedMillis = 0, laps = emptyList()) } },
            )
            ClockMode.TIMER -> {
                Text(
                    if (notificationsAllowed && exactAllowed && !TimerAlerts.schedulingFailed)
                        "Completion notifications use Android alarms. No ongoing notification is shown."
                    else "Background alerts are not guaranteed. Enable notifications and precise alarms for the best timing.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text("Android battery restrictions, Do Not Disturb and force-stop can delay or prevent alerts. An active timer is restored after reboot and unlock.",
                    style = MaterialTheme.typography.bodySmall)
                if (!notificationsAllowed) {
                    Text("Notification permission lets GearBox show and sound the completion alert after you leave this screen.",
                        style = MaterialTheme.typography.bodySmall)
                    if (Build.VERSION.SDK_INT >= 33) {
                        OutlinedButton(onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                            Text("Allow notifications")
                        }
                    }
                    OutlinedButton(onClick = {
                        runCatching { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }
                            .onFailure { error = "Open GearBox notification settings in Android Settings." }
                    }) { Text("Notification settings") }
                }
                if (!exactAllowed && Build.VERSION.SDK_INT >= 31) {
                    Text("Precise alarm access lets Android deliver near the deadline. Without it, a best-effort alert may be late.",
                        style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = {
                        runCatching { context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:${context.packageName}"))) }
                            .onFailure { error = "Open GearBox's Alarms & reminders access in Android Settings." }
                    }) { Text("Allow precise alarms") }
                }
                Spacer(Modifier.height(12.dp))
                TimerSection(
                    remainingMillis = state.timerRemainingMillis,
                    isRunning = state.timerRunning,
                    finished = state.timerFinished,
                    minutesInput = draftMinutes,
                    secondsInput = draftSeconds,
                    onMinutesChange = { value ->
                        draftMinutes = value
                        change(updateWidgets = false) { it.copy(minutesInput = value) }
                    },
                    onSecondsChange = { value ->
                        draftSeconds = value
                        change(updateWidgets = false) { it.copy(secondsInput = value) }
                    },
                    onStartPause = {
                        val requestedMinutes = draftMinutes
                        val requestedSeconds = draftSeconds
                        if (!state.timerRunning && state.timerRemainingMillis == 0L &&
                            timerDurationMillis(requestedMinutes, requestedSeconds) == null) {
                            error = "Enter 0–999 minutes and 0–59 seconds, with a total above zero."
                        } else change { toggleCountdown(it.copy(minutesInput = requestedMinutes, secondsInput = requestedSeconds)) }
                    },
                    onReset = { change { resetCountdown(it) } },
                )
            }
        }
    }
}

@Composable
private fun StopwatchSection(
    elapsedMillis: Long,
    isRunning: Boolean,
    laps: List<Long>,
    onToggleRunning: () -> Unit,
    onLap: () -> Unit,
    onReset: () -> Unit,
) {
    Text(
        text = formatDuration(elapsedMillis),
        style = MaterialTheme.typography.displayMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(Modifier.height(20.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(onClick = onToggleRunning, modifier = Modifier.weight(1f)) {
            Text(if (isRunning) "Pause" else "Start")
        }
        OutlinedButton(onClick = onLap, enabled = isRunning, modifier = Modifier.weight(1f)) {
            Text("Lap")
        }
        OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) {
            Text("Reset")
        }
    }

    if (laps.isNotEmpty()) {
        Spacer(Modifier.height(16.dp))
        laps.asReversed().forEachIndexed { index, lapTime ->
            Text(
                text = "Lap ${laps.size - index}: ${formatDuration(lapTime)}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun TimerSection(
    remainingMillis: Long,
    isRunning: Boolean,
    finished: Boolean,
    minutesInput: String,
    secondsInput: String,
    onMinutesChange: (String) -> Unit,
    onSecondsChange: (String) -> Unit,
    onStartPause: () -> Unit,
    onReset: () -> Unit,
) {
    val isEditable = !isRunning && remainingMillis == 0L
    if (isEditable) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = minutesInput,
                onValueChange = onMinutesChange,
                label = { Text("Minutes") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = secondsInput,
                onValueChange = onSecondsChange,
                label = { Text("Seconds") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(20.dp))
    }

    Text(
        text = formatDuration(remainingMillis),
        style = MaterialTheme.typography.displayMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )

    if (finished) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Time's up!",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    Spacer(Modifier.height(20.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(onClick = onStartPause, modifier = Modifier.weight(1f)) {
            Text(if (isRunning) "Pause" else "Start")
        }
        OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) {
            Text("Reset")
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val centis = (millis % 1000) / 10
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, centis)
    }
}
