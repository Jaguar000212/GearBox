package com.jaguar.gearbox.ui.tools

import com.jaguar.gearbox.ui.components.ToolFocusView
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
import com.jaguar.gearbox.tiles.ClockTileLaunch
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
import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf
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
import com.jaguar.gearbox.data.ToolMemoryStore
import com.jaguar.gearbox.logic.parseClockSetup
import com.jaguar.gearbox.logic.restoreClockSetup
import com.jaguar.gearbox.ui.components.rememberToolHistory
import com.jaguar.gearbox.ui.components.ToolHistoryBinding
import com.jaguar.gearbox.ui.components.LocalToolImport
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

val LocalClockNavigationTarget = staticCompositionLocalOf<State<String?>> { mutableStateOf(null) }

private enum class ClockMode(val label: Int) { STOPWATCH(R.string.game_stopwatch), TIMER(R.string.game_timer) }

@Composable
fun TimerStopwatchScreen(onNavigateBack: () -> Unit) = TimerStopwatchScreen(onNavigateBack, false)

@Composable
fun TimerStopwatchScreen(onNavigateBack: () -> Unit, requestStopwatchReset: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val mutations = remember { Mutex() }
    val memory = remember { ToolMemoryStore(context) }
    val clockNavigationTarget = LocalClockNavigationTarget.current
    val navigationTarget by clockNavigationTarget
    val tileLaunch = ClockTileLaunch.fromRoute(navigationTarget)
    var tileSelectionConsumed by rememberSaveable(navigationTarget) { mutableStateOf(false) }
    val tilePresentationOnly = tileLaunch != null && !tileSelectionConsumed
    var preferencesInitialized by rememberSaveable { mutableStateOf(false) }
    val userInteracted = remember { mutableStateOf(false) }
    var snapshotInputs by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var snapshotOutput by remember { mutableStateOf<String?>(null) }
    var snapshotMessage by remember { mutableStateOf<String?>(null) }
    var capturing by remember { mutableStateOf(false) }
    var state by remember { mutableStateOf(ClockRepository.snapshot(context)) }
    var draftMinutes by rememberSaveable { mutableStateOf(state.minutesInput) }
    var draftSeconds by rememberSaveable { mutableStateOf(state.secondsInput) }
    var showClockOptions by rememberSaveable { mutableStateOf(false) }
    var showFocus by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notificationsAllowed by remember { mutableStateOf(TimerAlerts.notificationsAllowed(context)) }
    var exactAllowed by remember { mutableStateOf(TimerAlerts.exactAlarmsAllowed(context)) }
    val selectedMode = ClockMode.entries.firstOrNull { it.name == if (tilePresentationOnly) tileLaunch?.mode else state.mode } ?: ClockMode.STOPWATCH

    fun change(updateWidgets: Boolean = true, transform: (ClockState) -> ClockState) {
        userInteracted.value = true
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
                error = context.getString(R.string.game_could_not_save_the_clock_state_please_try_again)
            }
        }
    }

    fun importSetup(values: Map<String, String>) {
        tileSelectionConsumed = true
        userInteracted.value = true
        val setup = parseClockSetup(values)
        if (setup == null) { error = context.getString(R.string.game_this_clock_setup_is_invalid_choose_a_valid_duration); return }
        scope.launch {
            var refused = false
            try {
                state = mutations.withLock { withContext(Dispatchers.IO) {
                    val updated = ClockRepository.update(context) { current ->
                        restoreClockSetup(current, setup) ?: current.also { refused = true }
                    }
                    TimerWidget().updateAll(context)
                    updated
                } }
                if (refused) error = context.getString(R.string.game_reset_running_or_paused_clocks_before_restoring_a_setup_ex)
                else {
                    draftMinutes = setup.minutes; draftSeconds = setup.seconds
                    snapshotInputs = emptyMap(); snapshotOutput = null
                    error = null
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { error = context.getString(R.string.game_could_not_restore_the_clock_setup_please_try_again) }
        }
    }
    val importedSetup = LocalToolImport.current
    LaunchedEffect(navigationTarget) {
        if (tileLaunch != null) {
            // Launch-only: the tile chooses presentation, not persisted mode or duration.
            preferencesInitialized = true
            return@LaunchedEffect
        }
        if (!preferencesInitialized) {
            try {
                val preference = withContext(Dispatchers.IO) {
                    memory.settings() to parseClockSetup(memory.loadMode(Tools.ROUTE_TIMER_STOPWATCH))
                }
                if (!userInteracted.value && importedSetup?.route != Tools.ROUTE_TIMER_STOPWATCH) {
                    state = mutations.withLock { withContext(Dispatchers.IO) {
                        ClockRepository.update(context) { current ->
                            if (userInteracted.value) current else
                                com.jaguar.gearbox.logic.applyClockPreferences(current, clockNavigationTarget.value,
                                    requestStopwatchReset, preference.first.rememberModes, preference.second)
                        }
                    } }
                    if (!userInteracted.value) { draftMinutes = state.minutesInput; draftSeconds = state.secondsInput }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { error = context.getString(R.string.game_could_not_load_clock_preferences_existing_clock_state_was) }
            finally { preferencesInitialized = true }
        }
    }
    val configuration = mapOf("mode" to selectedMode.name, "minutes" to draftMinutes, "seconds" to draftSeconds)
    val adapter = rememberToolHistory(Tools.ROUTE_TIMER_STOPWATCH, inputs = configuration, output = null,
        onRestore = ::importSetup, modeInputs = configuration.takeIf { !tilePresentationOnly && preferencesInitialized && parseClockSetup(it) != null }.orEmpty(),
        onRestoreMode = { values ->
            if ((!tilePresentationOnly || importedSetup?.route == Tools.ROUTE_TIMER_STOPWATCH) && com.jaguar.gearbox.logic.mayImportClockMode(importedSetup?.route == Tools.ROUTE_TIMER_STOPWATCH)) importSetup(values)
        },
        restoreLastResult = false)
    val history = ToolHistoryBinding(adapter.route, adapter.restore,
        inputs = snapshotInputs.ifEmpty { configuration }, output = snapshotOutput,
        configuration = configuration.takeIf { parseClockSetup(it) != null }.orEmpty())

    // The store is the only clock writer. Local text drafts update synchronously for fast typing;
    // polling cannot overwrite those drafts, and no disposal callback rewrites clock snapshots.
    LaunchedEffect(context, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            try {
                withContext(Dispatchers.IO) { ClockRepository.restoreAlarm(context) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                error = context.getString(R.string.game_could_not_restore_the_timer_alert_please_restart_the_timer)
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
                    error = context.getString(R.string.game_could_not_update_the_clock_state)
                }
                notificationsAllowed = TimerAlerts.notificationsAllowed(context)
                exactAllowed = TimerAlerts.exactAlarmsAllowed(context)
                delay(100)
            }
        }
    }


    fun toggleTimerFromView() {
        val requestedMinutes = draftMinutes
        val requestedSeconds = draftSeconds
        if (!state.timerRunning && state.timerRemainingMillis == 0L &&
            timerDurationMillis(requestedMinutes, requestedSeconds) == null) {
            error = context.getString(R.string.game_enter_0_999_minutes_and_0_59_seconds_with_a_total_above_ze)
        } else change { toggleCountdown(it.copy(minutesInput = requestedMinutes, secondsInput = requestedSeconds)) }
    }
    if (showFocus) {
        ToolFocusView(context.getString(selectedMode.label), onExit = { showFocus = false }) {
            val isTimer = selectedMode == ClockMode.TIMER
            val elapsed = if (isTimer) state.timerRemainingMillis else state.stopwatchElapsedMillis
            val running = if (isTimer) state.timerRunning else state.stopwatchRunning
            ClockFace(elapsed, when {
                isTimer && state.timerFinished -> context.getString(R.string.game_finished)
                running -> context.getString(R.string.game_running)
                elapsed > 0L -> context.getString(R.string.game_paused_ready)
                else -> context.getString(R.string.game_ready)
            })
            Button(onClick = {
                if (isTimer) toggleTimerFromView()
                else change { it.copy(stopwatchRunning = !it.stopwatchRunning) }
            }, modifier = Modifier.fillMaxWidth()) {
                Text(if (running) context.getString(R.string.game_pause) else if (elapsed > 0L) context.getString(R.string.game_resume) else context.getString(R.string.game_start))
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (isTimer && (!notificationsAllowed || !exactAllowed || TimerAlerts.schedulingFailed)) {
                Text(context.getString(R.string.game_alerts_need_attention_exit_focus_view_and_open_clock_optio), color = MaterialTheme.colorScheme.error)
            }
        }
    }
    if (showClockOptions) {
        com.jaguar.gearbox.ui.screens.ClockOptionsDialog { showClockOptions = false }
    }
    ToolScaffold(
        title = context.getString(R.string.game_timer_stopwatch),
        icon = Tools.byRoute(Tools.ROUTE_TIMER_STOPWATCH)!!.icon,
        onNavigateBack = onNavigateBack,
        history = history,
        options = {
            IconButton(onClick = { showFocus = true }) { Icon(Icons.Filled.Fullscreen, contentDescription = context.getString(R.string.game_focus_view)) }
            IconButton(onClick = { showClockOptions = true }) { Icon(Icons.Filled.Settings, contentDescription = context.getString(R.string.game_clock_options)) }
        },
    ) {
        TabRow(selectedTabIndex = ClockMode.entries.indexOf(selectedMode)) {
            ClockMode.entries.forEach { entry ->
                Tab(selected = entry == selectedMode, onClick = { tileSelectionConsumed = true; change { it.copy(mode = entry.name) } },
                    text = { Text(context.getString(entry.label)) })
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        TextButton(enabled = !capturing, onClick = {
            if (capturing) return@TextButton
            val historyGeneration = memory.historyGeneration(Tools.ROUTE_TIMER_STOPWATCH)
            capturing = true
            scope.launch {
                try {
                    val captured = withContext(Dispatchers.IO) { ClockRepository.snapshot(context) }
                    val inputs = mapOf("mode" to selectedMode.name, "minutes" to captured.minutesInput, "seconds" to captured.secondsInput)
                    val output = if (selectedMode == ClockMode.TIMER) {
                        context.getString(R.string.game_timer_snapshot_1_s_remaining, formatDuration(captured.timerRemainingMillis)) +
                            if (captured.timerFinished) context.getString(R.string.game_finished) else if (captured.timerRunning) context.getString(R.string.game_running) else context.getString(R.string.game_paused_ready)
                    } else stopwatchResultText(captured.stopwatchElapsedMillis, captured.laps, context)
                    snapshotInputs = inputs; snapshotOutput = output
                    val recorded = withContext(Dispatchers.IO) { memory.record(Tools.ROUTE_TIMER_STOPWATCH, inputs, output, expectedGeneration = historyGeneration) }
                    snapshotMessage = if (recorded) context.getString(R.string.game_snapshot_added_to_history_restore_loads_setup_only_never_e)
                        else context.getString(R.string.game_snapshot_ready_in_history_keep_result_it_was_not_added_to)
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { error = context.getString(R.string.game_could_not_capture_the_clock_snapshot) }
                finally { capturing = false }
            }
        }) { Text(context.getString(R.string.game_capture_snapshot)) }
        snapshotMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
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
                        Text(context.getString(R.string.game_alerts_need_attention_fix), color = MaterialTheme.colorScheme.error)
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
                    onStartPause = { toggleTimerFromView() },
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
            title = context.getString(R.string.game_reset_stopwatch),
            message = context.getString(R.string.game_elapsed_time_and_all_recorded_laps_will_be_cleared),
            onDismiss = { confirmReset = false },
            onConfirm = { confirmReset = false; onReset() },
        )
    }
    ClockFace(elapsedMillis, when {
        isRunning -> context.getString(R.string.game_running)
        elapsedMillis > 0L -> context.getString(R.string.game_paused)
        else -> context.getString(R.string.game_ready)
    })

    Spacer(Modifier.height(20.dp))
    Button(onClick = onToggleRunning, modifier = Modifier.fillMaxWidth()) {
        Text(if (isRunning) context.getString(R.string.game_pause) else if (elapsedMillis > 0L) context.getString(R.string.game_resume) else context.getString(R.string.game_start))
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onLap, enabled = isRunning, modifier = Modifier.weight(1f)) {
            Text(context.getString(R.string.game_lap))
        }
        OutlinedButton(onClick = { confirmReset = true }, enabled = isRunning || elapsedMillis > 0L || laps.isNotEmpty(), modifier = Modifier.weight(1f)) {
            Text(context.getString(R.string.game_reset_45))
        }
    }

    if (elapsedMillis > 0L || laps.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        ToolActions(
            onCopy = { if (!isRunning) context.copyToClipboard(context.getString(R.string.game_stopwatch), stopwatchResultText(elapsedMillis, laps, context)) },
            onShare = { if (!isRunning) context.shareText(stopwatchResultText(elapsedMillis, laps, context)) },
            resultEnabled = !isRunning,
        )
        if (isRunning) Text(context.getString(R.string.game_pause_to_copy_or_share_the_elapsed_time_and_laps),
            style = MaterialTheme.typography.bodySmall)
    }

    if (laps.isNotEmpty()) {
        Spacer(Modifier.height(16.dp))
        Text(context.getString(R.string.game_laps), style = MaterialTheme.typography.titleMedium)
        laps.indices.reversed().forEach { index ->
            val total = laps[index]
            val split = total - (laps.getOrNull(index - 1) ?: 0L)
            Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(context.getString(R.string.game_lap_1_s, index + 1), style = MaterialTheme.typography.labelLarge)
                    Text(context.getString(R.string.game_split_1_s, formatDuration(split)), style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace)
                    Text(context.getString(R.string.game_total_1_s, formatDuration(total)), style = MaterialTheme.typography.bodyMedium,
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
    val context = LocalContext.current
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    if (confirmReset) {
        ClockResetDialog(
            title = context.getString(R.string.game_reset_timer),
            message = context.getString(R.string.game_the_current_countdown_will_be_stopped_and_cleared),
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
                label = { Text(context.getString(R.string.game_minutes)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = secondsInput,
                onValueChange = onSecondsChange,
                label = { Text(context.getString(R.string.game_seconds)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1, 5, 10).forEach { minutes ->
                TextButton(onClick = { onMinutesChange(minutes.toString()); onSecondsChange("0") },
                    modifier = Modifier.weight(1f)) { Text(context.getString(R.string.game_1_s_min, minutes)) }
            }
        }
        Spacer(Modifier.height(20.dp))
    }

    ClockFace(remainingMillis, when {
        finished -> context.getString(R.string.game_time_s_up)
        isRunning -> context.getString(R.string.game_running)
        remainingMillis > 0L -> context.getString(R.string.game_paused)
        else -> context.getString(R.string.game_ready)
    })

    Spacer(Modifier.height(20.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(onClick = onStartPause, modifier = Modifier.weight(1f)) {
            Text(when { isRunning -> context.getString(R.string.game_pause); remainingMillis > 0L -> context.getString(R.string.game_resume); finished -> context.getString(R.string.game_start_again); else -> context.getString(R.string.game_start) })
        }
        OutlinedButton(onClick = { if (isRunning || remainingMillis > 0L) confirmReset = true else onReset() }, modifier = Modifier.weight(1f)) {
            Text(context.getString(R.string.game_reset_45))
        }
    }
}

@Composable
private fun ClockResetDialog(title: String, message: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(context.getString(R.string.game_reset_now)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(context.getString(R.string.game_keep_timing)) } },
    )
}

/** Stable snapshot export; callers expose it only when the stopwatch is paused. */
internal fun stopwatchResultText(elapsedMillis: Long, laps: List<Long>, context: android.content.Context? = null): String = buildString {
    append(context?.getString(R.string.game_stopwatch_export, formatDuration(elapsedMillis)) ?: "Stopwatch: ${formatDuration(elapsedMillis)}")
    laps.forEachIndexed { index, total ->
        val split = total - (laps.getOrNull(index - 1) ?: 0L)
        append(context?.getString(R.string.game_lap_export, index + 1, formatDuration(split), formatDuration(total))
            ?: "\nLap ${index + 1}: split ${formatDuration(split)}, total ${formatDuration(total)}")
    }
}

/** Stable digits without animated transitions; wraps long durations at large font scales. */
@Composable
private fun ClockFace(millis: Long, status: String) {
    val context = LocalContext.current
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
                        contentDescription = clockDurationDescription(context, seconds)
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

/** Formats accessibility duration only; it never reads or changes a running clock. */
internal fun clockDurationDescription(context: android.content.Context, seconds: Long): String {
    fun quantity(resource: Int, count: Long): String = context.resources.getQuantityString(
        resource, count.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), count)
    return context.getString(R.string.game_duration_description,
        quantity(R.plurals.game_hours, seconds / 3600),
        quantity(R.plurals.game_minutes, (seconds % 3600) / 60),
        quantity(R.plurals.game_seconds, seconds % 60))
}
