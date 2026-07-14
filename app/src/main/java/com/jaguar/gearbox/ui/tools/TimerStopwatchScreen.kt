package com.jaguar.gearbox.ui.tools

import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.SimplePrefsStore
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.ToolScaffold
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

private enum class ClockMode(val label: String) {
    STOPWATCH("Stopwatch"),
    TIMER("Timer"),
}

private val lapsSaver: Saver<MutableState<List<Long>>, LongArray> = Saver(
    save = { it.value.toLongArray() },
    restore = { mutableStateOf(it.toList()) },
)

private const val KEY_MODE = "timer_stopwatch.mode"
private const val KEY_SW_ELAPSED = "timer_stopwatch.sw_elapsed"
private const val KEY_SW_RUNNING = "timer_stopwatch.sw_running"
private const val KEY_LAPS = "timer_stopwatch.laps"
private const val KEY_TIMER_REMAINING = "timer_stopwatch.timer_remaining"
private const val KEY_TIMER_RUNNING = "timer_stopwatch.timer_running"
private const val KEY_TIMER_FINISHED = "timer_stopwatch.timer_finished"
private const val KEY_MINUTES = "timer_stopwatch.minutes_input"
private const val KEY_SECONDS = "timer_stopwatch.seconds_input"
private const val KEY_SAVED_AT = "timer_stopwatch.saved_at"

/** Snapshot of everything this screen needs to restore, with running clocks caught up to now. */
private class TimerLoadResult(
    val mode: String,
    val stopwatchElapsedMillis: Long,
    val stopwatchRunning: Boolean,
    val laps: List<Long>,
    val timerRemainingMillis: Long,
    val timerRunning: Boolean,
    val timerFinished: Boolean,
    val minutesInput: String,
    val secondsInput: String,
)

/**
 * Loads persisted state and, if a clock was left running, advances it by however long has
 * passed since the last save — so leaving the screen (or killing the app) no longer pauses a
 * stopwatch/timer that the user believed was still counting.
 */
private fun loadTimerState(store: SimplePrefsStore): TimerLoadResult {
    val mode = store.getString(KEY_MODE, ClockMode.STOPWATCH.name)
    var stopwatchElapsed = store.getLong(KEY_SW_ELAPSED, 0L)
    val stopwatchRunning = store.getBoolean(KEY_SW_RUNNING, false)
    val laps = store.getLongList(KEY_LAPS)
    var timerRemaining = store.getLong(KEY_TIMER_REMAINING, 0L)
    var timerRunning = store.getBoolean(KEY_TIMER_RUNNING, false)
    var timerFinished = store.getBoolean(KEY_TIMER_FINISHED, false)
    val minutesInput = store.getString(KEY_MINUTES, "5")
    val secondsInput = store.getString(KEY_SECONDS, "0")
    val savedAt = store.getLong(KEY_SAVED_AT, 0L)

    if (savedAt > 0L) {
        val gap = (System.currentTimeMillis() - savedAt).coerceAtLeast(0L)
        if (stopwatchRunning) stopwatchElapsed += gap
        if (timerRunning) {
            timerRemaining = (timerRemaining - gap).coerceAtLeast(0L)
            if (timerRemaining == 0L) {
                timerRunning = false
                timerFinished = true
            }
        }
    }

    return TimerLoadResult(
        mode = mode,
        stopwatchElapsedMillis = stopwatchElapsed,
        stopwatchRunning = stopwatchRunning,
        laps = laps,
        timerRemainingMillis = timerRemaining,
        timerRunning = timerRunning,
        timerFinished = timerFinished,
        minutesInput = minutesInput,
        secondsInput = secondsInput,
    )
}

@Composable
fun TimerStopwatchScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { SimplePrefsStore(context) }
    val initial = remember { loadTimerState(store) }

    var mode by rememberSaveable { mutableStateOf(initial.mode) }
    val selectedMode = ClockMode.valueOf(mode)

    // All clock state (and the single ticking effect below) lives here, above the tab switch,
    // so that showing the Timer tab can never pause the Stopwatch's ticking (or vice versa) --
    // that was the previous bug: each tab's LaunchedEffect only ran while its composable was in
    // the active `when` branch, so the other clock silently froze while claiming to be running.
    var stopwatchElapsedMillis by rememberSaveable { mutableLongStateOf(initial.stopwatchElapsedMillis) }
    var stopwatchRunning by rememberSaveable { mutableStateOf(initial.stopwatchRunning) }
    var laps by rememberSaveable(saver = lapsSaver) { mutableStateOf(initial.laps) }

    var timerRemainingMillis by rememberSaveable { mutableLongStateOf(initial.timerRemainingMillis) }
    var timerRunning by rememberSaveable { mutableStateOf(initial.timerRunning) }
    var timerFinished by rememberSaveable { mutableStateOf(initial.timerFinished) }
    var minutesInput by rememberSaveable { mutableStateOf(initial.minutesInput) }
    var secondsInput by rememberSaveable { mutableStateOf(initial.secondsInput) }

    fun persistState() {
        store.edit {
            putString(KEY_MODE, mode)
            putLong(KEY_SW_ELAPSED, stopwatchElapsedMillis)
            putBoolean(KEY_SW_RUNNING, stopwatchRunning)
            putString(KEY_LAPS, laps.joinToString(","))
            putLong(KEY_TIMER_REMAINING, timerRemainingMillis)
            putBoolean(KEY_TIMER_RUNNING, timerRunning)
            putBoolean(KEY_TIMER_FINISHED, timerFinished)
            putString(KEY_MINUTES, minutesInput)
            putString(KEY_SECONDS, secondsInput)
            putLong(KEY_SAVED_AT, System.currentTimeMillis())
        }
    }

    // Saves whatever state exists the moment this screen leaves composition (back-navigation),
    // so a running clock isn't silently forgotten even if the user never pauses it first.
    DisposableEffect(Unit) {
        onDispose { persistState() }
    }

    LaunchedEffect(stopwatchRunning, timerRunning) {
        var lastTick = System.currentTimeMillis()
        var millisSinceSave = 0L
        while (stopwatchRunning || timerRunning) {
            delay(30.milliseconds)
            val now = System.currentTimeMillis()
            val delta = now - lastTick
            lastTick = now

            if (stopwatchRunning) {
                stopwatchElapsedMillis += delta
            }
            if (timerRunning) {
                timerRemainingMillis = (timerRemainingMillis - delta).coerceAtLeast(0)
                if (timerRemainingMillis == 0L) {
                    timerRunning = false
                    timerFinished = true
                    playTimerAlert(context)
                    persistState()
                }
            }

            // Persist periodically (not every 30ms tick) so a killed process loses at most ~1s
            // of progress instead of freezing the clock at whatever it read on the last visit.
            millisSinceSave += delta
            if (millisSinceSave >= 1000L) {
                millisSinceSave = 0L
                persistState()
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
                Tab(
                    selected = entry == selectedMode,
                    onClick = { mode = entry.name; persistState() },
                    text = { Text(entry.label) },
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        when (selectedMode) {
            ClockMode.STOPWATCH -> StopwatchSection(
                elapsedMillis = stopwatchElapsedMillis,
                isRunning = stopwatchRunning,
                laps = laps,
                onToggleRunning = { stopwatchRunning = !stopwatchRunning; persistState() },
                onLap = { laps = laps + stopwatchElapsedMillis; persistState() },
                onReset = {
                    stopwatchRunning = false
                    stopwatchElapsedMillis = 0L
                    laps = emptyList()
                    persistState()
                },
            )
            ClockMode.TIMER -> TimerSection(
                remainingMillis = timerRemainingMillis,
                isRunning = timerRunning,
                finished = timerFinished,
                minutesInput = minutesInput,
                secondsInput = secondsInput,
                onMinutesChange = { minutesInput = it },
                onSecondsChange = { secondsInput = it },
                onStartPause = {
                    if (timerRunning) {
                        timerRunning = false
                    } else {
                        if (timerRemainingMillis == 0L) {
                            val minutes = minutesInput.trim().toLongOrNull()?.coerceIn(0, 999) ?: 0
                            val seconds = secondsInput.trim().toLongOrNull()?.coerceIn(0, 59) ?: 0
                            timerRemainingMillis = (minutes * 60 + seconds) * 1000
                            timerFinished = false
                        }
                        if (timerRemainingMillis > 0) timerRunning = true
                    }
                    persistState()
                },
                onReset = {
                    timerRunning = false
                    timerRemainingMillis = 0L
                    timerFinished = false
                    persistState()
                },
            )
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

/** Best-effort alert sound + vibration when a timer finishes; a silent failure isn't fatal. */
private fun playTimerAlert(context: Context) {
    try {
        val uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        RingtoneManager.getRingtone(context, uri)?.play()
    } catch (e: Exception) {
        // Some devices/emulators have no ringtone configured, or a flaky audio service.
    }

    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
    vibrator?.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
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
