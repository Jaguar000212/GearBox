package com.jaguar.gearbox.ui.tools

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

@Composable
fun TimerStopwatchScreen(onNavigateBack: () -> Unit) {
    var mode by rememberSaveable { mutableStateOf(ClockMode.STOPWATCH.name) }
    val selectedMode = ClockMode.valueOf(mode)

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
            ClockMode.STOPWATCH -> StopwatchSection()
            ClockMode.TIMER -> TimerSection()
        }
    }
}

private val lapsSaver: Saver<MutableState<List<Long>>, LongArray> = Saver(
    save = { it.value.toLongArray() },
    restore = { mutableStateOf(it.toList()) },
)

@Composable
private fun StopwatchSection() {
    var elapsedMillis by rememberSaveable { mutableLongStateOf(0L) }
    var isRunning by rememberSaveable { mutableStateOf(false) }
    var laps by rememberSaveable(saver = lapsSaver) { mutableStateOf(emptyList()) }

    LaunchedEffect(isRunning) {
        var lastTick = System.currentTimeMillis()
        while (isRunning) {
            delay(30.milliseconds)
            val now = System.currentTimeMillis()
            elapsedMillis += now - lastTick
            lastTick = now
        }
    }

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
        Button(
            onClick = { isRunning = !isRunning },
            modifier = Modifier.weight(1f),
        ) { Text(if (isRunning) "Pause" else "Start") }
        OutlinedButton(
            onClick = { if (isRunning) laps = laps + elapsedMillis },
            enabled = isRunning,
            modifier = Modifier.weight(1f),
        ) { Text("Lap") }
        OutlinedButton(
            onClick = {
                isRunning = false
                elapsedMillis = 0L
                laps = emptyList()
            },
            modifier = Modifier.weight(1f),
        ) { Text("Reset") }
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
private fun TimerSection() {
    var minutesInput by rememberSaveable { mutableStateOf("5") }
    var secondsInput by rememberSaveable { mutableStateOf("0") }
    var remainingMillis by rememberSaveable { mutableLongStateOf(0L) }
    var isRunning by rememberSaveable { mutableStateOf(false) }
    var finished by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isRunning) {
        var lastTick = System.currentTimeMillis()
        while (isRunning) {
            delay(30.milliseconds)
            val now = System.currentTimeMillis()
            val delta = now - lastTick
            lastTick = now
            remainingMillis = (remainingMillis - delta).coerceAtLeast(0)
            if (remainingMillis == 0L) {
                isRunning = false
                finished = true
            }
        }
    }

    val isEditable = !isRunning && remainingMillis == 0L
    if (isEditable) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = minutesInput,
                onValueChange = { minutesInput = it },
                label = { Text("Minutes") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = secondsInput,
                onValueChange = { secondsInput = it },
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
        Button(
            onClick = {
                if (isRunning) {
                    isRunning = false
                } else {
                    if (remainingMillis == 0L) {
                        val minutes = minutesInput.trim().toLongOrNull()?.coerceIn(0, 999) ?: 0
                        val seconds = secondsInput.trim().toLongOrNull()?.coerceIn(0, 59) ?: 0
                        remainingMillis = (minutes * 60 + seconds) * 1000
                        finished = false
                    }
                    if (remainingMillis > 0) isRunning = true
                }
            },
            modifier = Modifier.weight(1f),
        ) { Text(if (isRunning) "Pause" else "Start") }
        OutlinedButton(
            onClick = {
                isRunning = false
                remainingMillis = 0L
                finished = false
            },
            modifier = Modifier.weight(1f),
        ) { Text("Reset") }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val centis = (millis % 1000) / 10
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d.%02d", minutes, seconds, centis)
    }
}
