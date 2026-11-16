package com.jaguar.gearbox.ui.tools

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Switch
import androidx.compose.ui.res.stringResource
import com.jaguar.gearbox.R
import com.jaguar.gearbox.timer.TimerStatusNotification
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
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
import com.jaguar.gearbox.ui.components.ToolActions
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
fun TimerStopwatchScreen(onNavigateBack: () -> Unit) = TimerStopwatchScreen(onNavigateBack, false)

@Composable
fun TimerStopwatchScreen(onNavigateBack: () -> Unit, requestStopwatchReset: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val mutations = remember { Mutex() }
    var state by remember { mutableStateOf(ClockRepository.snapshot(context)) }
    var draftMinutes by rememberSaveable { mutableStateOf(state.minutesInput) }
    var draftSeconds by rememberSaveable { mutableStateOf(state.secondsInput) }
    var showClockOptions by rememberSaveable { mutableStateOf(false) }
    var showFocus by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notificationsAllowed by remember { mutableStateOf(TimerAlerts.notificationsAllowed(context)) }
    var exactAllowed by remember { mutableStateOf(TimerAlerts.exactAlarmsAllowed(context)) }
    val selectedMode = ClockMode.entries.firstOrNull { it.name == state.mode } ?: ClockMode.STOPWATCH

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


    if (showFocus) {
        Dialog(onDismissRequest = { showFocus = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(selectedMode.label, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(24.dp))
                    val displayMillis = if (selectedMode == ClockMode.TIMER) state.timerRemainingMillis else state.stopwatchElapsedMillis
                    Text(formatDuration(displayMillis), style = MaterialTheme.typography.displayLarge,
                        fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().clearAndSetSemantics {
                            val seconds = displayMillis / 1000
                            contentDescription = "${seconds / 3600} hours, ${(seconds % 3600) / 60} minutes, ${seconds % 60} seconds"
                        })
                    Spacer(Modifier.height(24.dp))
                    TextButton(onClick = { showFocus = false }) { Text("Exit focus view") }
                }
            }
        }
    }
    if (showClockOptions) {
        com.jaguar.gearbox.ui.screens.ClockOptionsDialog { showClockOptions = false }
    }
    ToolScaffold(
        title = "Timer / Stopwatch",
        icon = Tools.byRoute(Tools.ROUTE_TIMER_STOPWATCH)!!.icon,
        onNavigateBack = onNavigateBack,
        options = {
            IconButton(onClick = { showFocus = true }) { Icon(Icons.Filled.Fullscreen, contentDescription = "Focus view") }
            IconButton(onClick = { showClockOptions = true }) { Icon(Icons.Filled.Settings, contentDescription = "Clock options") }
        },
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
                requestReset = requestStopwatchReset,
            )
            ClockMode.TIMER -> {
                val alertsReady = notificationsAllowed && exactAllowed && !TimerAlerts.schedulingFailed
                if (!alertsReady) {
                    TextButton(onClick = { showClockOptions = true }) {
                        Text("Alerts need attention · Fix", color = MaterialTheme.colorScheme.error)
                    }
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
internal fun StopwatchSection(
    elapsedMillis: Long,
    isRunning: Boolean,
    laps: List<Long>,
    onToggleRunning: () -> Unit,
    onLap: () -> Unit,
    onReset: () -> Unit,
    requestReset: Boolean = false,
) {
    val context = LocalContext.current
    var confirmReset by rememberSaveable { mutableStateOf(requestReset && (isRunning || elapsedMillis > 0L || laps.isNotEmpty())) }
    if (confirmReset) {
        ClockResetDialog(
            title = "Reset stopwatch?",
            message = "Elapsed time and all recorded laps will be cleared.",
            onDismiss = { confirmReset = false },
            onConfirm = { confirmReset = false; onReset() },
        )
    }
    ClockFace(elapsedMillis, when {
        isRunning -> "Running"
        elapsedMillis > 0L -> "Paused"
        else -> "Ready"
    })

    Spacer(Modifier.height(20.dp))
    Button(onClick = onToggleRunning, modifier = Modifier.fillMaxWidth()) {
        Text(if (isRunning) "Pause" else if (elapsedMillis > 0L) "Resume" else "Start")
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onLap, enabled = isRunning, modifier = Modifier.weight(1f)) {
            Text("Lap")
        }
        OutlinedButton(onClick = { confirmReset = true }, enabled = isRunning || elapsedMillis > 0L || laps.isNotEmpty(), modifier = Modifier.weight(1f)) {
            Text("Reset")
        }
    }

    if (elapsedMillis > 0L || laps.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        ToolActions(
            onCopy = { if (!isRunning) context.copyToClipboard("Stopwatch", stopwatchResultText(elapsedMillis, laps)) },
            onShare = { if (!isRunning) context.shareText(stopwatchResultText(elapsedMillis, laps)) },
            resultEnabled = !isRunning,
        )
        if (isRunning) Text("Pause to copy or share the elapsed time and laps.",
            style = MaterialTheme.typography.bodySmall)
    }

    if (laps.isNotEmpty()) {
        Spacer(Modifier.height(16.dp))
        Text("Laps", style = MaterialTheme.typography.titleMedium)
        laps.indices.reversed().forEach { index ->
            val total = laps[index]
            val split = total - (laps.getOrNull(index - 1) ?: 0L)
            Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Lap ${index + 1}", style = MaterialTheme.typography.labelLarge)
                    Text("Split ${formatDuration(split)}", style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace)
                    Text("Total ${formatDuration(total)}", style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
internal fun TimerSection(
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
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    if (confirmReset) {
        ClockResetDialog(
            title = "Reset timer?",
            message = "The current countdown will be stopped and cleared.",
            onDismiss = { confirmReset = false },
            onConfirm = { confirmReset = false; onReset() },
        )
    }
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
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = secondsInput,
                onValueChange = onSecondsChange,
                label = { Text("Seconds") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1, 5, 10).forEach { minutes ->
                TextButton(onClick = { onMinutesChange(minutes.toString()); onSecondsChange("0") },
                    modifier = Modifier.weight(1f)) { Text("$minutes min") }
            }
        }
        Spacer(Modifier.height(20.dp))
    }

    ClockFace(remainingMillis, when {
        finished -> "Time's up!"
        isRunning -> "Running"
        remainingMillis > 0L -> "Paused"
        else -> "Ready"
    })

    Spacer(Modifier.height(20.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(onClick = onStartPause, modifier = Modifier.weight(1f)) {
            Text(when { isRunning -> "Pause"; remainingMillis > 0L -> "Resume"; finished -> "Start again"; else -> "Start" })
        }
        OutlinedButton(onClick = { if (isRunning || remainingMillis > 0L) confirmReset = true else onReset() }, modifier = Modifier.weight(1f)) {
            Text("Reset")
        }
    }
}

@Composable
private fun ClockResetDialog(title: String, message: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Reset now") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Keep timing") } },
    )
}

/** Stable snapshot export; callers expose it only when the stopwatch is paused. */
internal fun stopwatchResultText(elapsedMillis: Long, laps: List<Long>): String = buildString {
    append("Stopwatch: ").append(formatDuration(elapsedMillis))
    laps.forEachIndexed { index, total ->
        val split = total - (laps.getOrNull(index - 1) ?: 0L)
        append("\nLap ").append(index + 1)
            .append(": split ").append(formatDuration(split))
            .append(", total ").append(formatDuration(total))
    }
}

/** Stable digits without animated transitions; wraps long durations at large font scales. */
@Composable
private fun ClockFace(millis: Long, status: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(status, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                Text(formatDuration(millis),
                    style = if (maxWidth < 320.dp) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.displayMedium,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clearAndSetSemantics {
                        // Do not make continuously ticking centiseconds a live announcement.
                        val seconds = millis / 1000
                        contentDescription = "${seconds / 3600} hours, ${(seconds % 3600) / 60} minutes, ${seconds % 60} seconds"
                    })
            }
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
