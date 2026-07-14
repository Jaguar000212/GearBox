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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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

@Composable
fun TimerStopwatchScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var mode by rememberSaveable { mutableStateOf(ClockMode.STOPWATCH.name) }
    val selectedMode = ClockMode.valueOf(mode)

    // All clock state (and the single ticking effect below) lives here, above the tab switch,
    // so that showing the Timer tab can never pause the Stopwatch's ticking (or vice versa) --
    // that was the previous bug: each tab's LaunchedEffect only ran while its composable was in
    // the active `when` branch, so the other clock silently froze while claiming to be running.
    var stopwatchElapsedMillis by rememberSaveable { mutableLongStateOf(0L) }
    var stopwatchRunning by rememberSaveable { mutableStateOf(false) }
    var laps by rememberSaveable(saver = lapsSaver) { mutableStateOf(emptyList()) }

    var timerRemainingMillis by rememberSaveable { mutableLongStateOf(0L) }
    var timerRunning by rememberSaveable { mutableStateOf(false) }
    var timerFinished by rememberSaveable { mutableStateOf(false) }
    var minutesInput by rememberSaveable { mutableStateOf("5") }
    var secondsInput by rememberSaveable { mutableStateOf("0") }

    LaunchedEffect(stopwatchRunning, timerRunning) {
        var lastTick = System.currentTimeMillis()
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
                }
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
                    onClick = { mode = entry.name },
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
                onToggleRunning = { stopwatchRunning = !stopwatchRunning },
                onLap = { laps = laps + stopwatchElapsedMillis },
                onReset = {
                    stopwatchRunning = false
                    stopwatchElapsedMillis = 0L
                    laps = emptyList()
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
                },
                onReset = {
                    timerRunning = false
                    timerRemainingMillis = 0L
                    timerFinished = false
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
