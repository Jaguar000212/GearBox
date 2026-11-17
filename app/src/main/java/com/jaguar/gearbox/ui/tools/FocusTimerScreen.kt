package com.jaguar.gearbox.ui.tools

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.jaguar.gearbox.R
import com.jaguar.gearbox.focus.*
import com.jaguar.gearbox.ui.components.AdaptiveToolLayout
import com.jaguar.gearbox.ui.components.CompactChoiceSelector
import com.jaguar.gearbox.ui.components.ToolScaffold
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Setup edits never mutate a live session. Navigation and restoration never start a session. */
@Composable
fun FocusTimerScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val repository = remember(context) { FocusRepository(context) }
    val scope = rememberCoroutineScope()
    val lock = remember { Mutex() }
    var state by remember { mutableStateOf<FocusState?>(null) }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var notificationsAllowed by remember { mutableStateOf(runCatching { FocusAlerts.notificationsAllowed(context) }.getOrDefault(false)) }
    var exactAlarmsAllowed by remember { mutableStateOf(runCatching { FocusAlerts.exactAlarmsAllowed(context) }.getOrDefault(false)) }
    var scheduleFailed by remember { mutableStateOf(false) }
    var modeName by rememberSaveable { mutableStateOf(FocusMode.POMODORO.name) }
    var work by rememberSaveable { mutableStateOf("1500") }
    var rest by rememberSaveable { mutableStateOf("300") }
    var longBreak by rememberSaveable { mutableStateOf("900") }
    var rounds by rememberSaveable { mutableStateOf("4") }
    var every by rememberSaveable { mutableStateOf("4") }
    var setupLoaded by rememberSaveable { mutableStateOf(false) }
    var replaceConfig by remember { mutableStateOf<Pair<FocusConfig, Long>?>(null) }
    var resetConfirm by remember { mutableStateOf(false) }
    var resetGeneration by remember { mutableStateOf<Long?>(null) }
    var permissionPending by remember { mutableStateOf(false) }
    val mode = FocusMode.entries.firstOrNull { it.name == modeName } ?: FocusMode.POMODORO
    val config = run {
        val w = work.toIntOrNull(); val r = rest.toIntOrNull(); val l = longBreak.toIntOrNull()
        val n = rounds.toIntOrNull(); val e = every.toIntOrNull()
        if (w == null || r == null || n == null || (mode == FocusMode.POMODORO && (l == null || e == null))) null
        else FocusConfig(mode, w, r, if (mode == FocusMode.POMODORO) l!! else 900, n,
            if (mode == FocusMode.POMODORO) e!! else 4).takeIf { it.isValid() }
    }
    fun updateAlerts() {
        notificationsAllowed = runCatching { FocusAlerts.notificationsAllowed(context) }.getOrDefault(false)
        exactAlarmsAllowed = runCatching { FocusAlerts.exactAlarmsAllowed(context) }.getOrDefault(false)
        scheduleFailed = FocusAlerts.schedulingFailed
    }
    fun act(action: () -> FocusState) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                lock.withLock { state = withContext(Dispatchers.IO) { action() } }
                failed = false
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                failed = true
                // A side effect can fail after persistence; display the authoritative state if readable.
                try { lock.withLock { state = withContext(Dispatchers.IO) { repository.refresh() } } }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { state = null /* Explicit confirmed Reset can recover unreadable storage. */ }
            }
            finally { busy = false; updateAlerts() }
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionPending = false
        updateAlerts() // Permission results never start or resume a session.
    }
    fun withNotificationPermission(action: () -> Unit) {
        if (permissionPending || busy) return
        // Act directly on the explicit button press, not on a later permission callback.
        // Denial does not silently cancel the user's session; the visible warning explains alerts.
        action()
        if (Build.VERSION.SDK_INT >= 33 && androidx.core.content.ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissionPending = true
            try { permission.launch(Manifest.permission.POST_NOTIFICATIONS) }
            catch (_: Exception) { permissionPending = false; failed = true }
        }
    }
    fun start(configToStart: FocusConfig, expectedGeneration: Long, replacementApproved: Boolean = false) {
        withNotificationPermission {
            // Recheck current state before replacing any active session.
            act {
                val current = repository.refresh()
                if (current.isActive && !replacementApproved) {
                    // Fail closed if a new session appeared since the button was rendered.
                    throw IllegalStateException("Active session requires replacement confirmation")
                }
                repository.start(configToStart, expectedGeneration = expectedGeneration)
            }
        }
    }
    LaunchedEffect(repository, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var firstRefresh = true
            while (true) {
                try {
                    lock.withLock { state = withContext(Dispatchers.IO) { if (firstRefresh) repository.read() else repository.refresh() } }
                    firstRefresh = false
                    if (!setupLoaded) {
                        state?.config?.let {
                            modeName = it.mode.name; work = it.workSeconds.toString(); rest = it.shortBreakSeconds.toString()
                            longBreak = it.longBreakSeconds.toString(); rounds = it.rounds.toString(); every = it.longBreakEvery.toString()
                        }
                        setupLoaded = true
                    }
                    updateAlerts()
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { failed = true; state = null }
                delay(1000)
            }
        }
    }
    val enabled = state != null && !busy && !permissionPending
    ToolScaffold(stringResource(R.string.focus_screen_title), Icons.Filled.Timelapse, onNavigateBack) {
        AdaptiveToolLayout(input = {
            Text(stringResource(R.string.focus_setup), style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() })
            CompactChoiceSelector(stringResource(R.string.focus_mode), mode, FocusMode.entries,
                { context.getString(if (it == FocusMode.POMODORO) R.string.focus_pomodoro else R.string.focus_intervals) },
                { if (enabled) modeName = it.name })
            FocusNumberField(work, { work = it }, R.string.focus_work_seconds, enabled)
            FocusNumberField(rest, { rest = it }, R.string.focus_rest_seconds, enabled)
            FocusNumberField(rounds, { rounds = it }, R.string.focus_rounds, enabled)
            if (mode == FocusMode.POMODORO) {
                FocusNumberField(longBreak, { longBreak = it }, R.string.focus_long_seconds, enabled)
                FocusNumberField(every, { every = it }, R.string.focus_long_every, enabled)
            }
            Text(stringResource(R.string.focus_setup_note), style = MaterialTheme.typography.bodySmall)
            if (config == null) Text(stringResource(R.string.focus_invalid), color = MaterialTheme.colorScheme.error)
            Button(enabled = enabled && config != null, onClick = {
                config?.let { setup -> state?.let { observed ->
                    if (observed.isActive) replaceConfig = setup to observed.generation
                    else start(setup, observed.generation)
                } }
            }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.focus_start)) }
        }, result = {
            Text(stringResource(R.string.focus_session_heading), style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() })
            val current = state
            if (current == null) {
                Text(stringResource(R.string.focus_loading))
                if (failed) OutlinedButton(enabled = !busy && !permissionPending,
                    onClick = { resetGeneration = null; resetConfirm = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.focus_reset))
                }
            }
            else {
                Text(stringResource(when (current.status) {
                    FocusStatus.IDLE -> R.string.focus_status_idle
                    FocusStatus.RUNNING -> R.string.focus_status_running
                    FocusStatus.PAUSED -> R.string.focus_status_paused
                    FocusStatus.FINISHED -> R.string.focus_status_finished
                    FocusStatus.INTERRUPTED -> R.string.focus_status_interrupted
                }), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                if (current.status != FocusStatus.IDLE) {
                    Text(context.getString(if (current.config.mode == FocusMode.POMODORO) R.string.focus_pomodoro else R.string.focus_intervals))
                    Text(stringResource(when (current.phase.kind) {
                        FocusPhaseKind.WORK -> R.string.focus_phase_work
                        FocusPhaseKind.SHORT_BREAK -> R.string.focus_phase_short
                        FocusPhaseKind.LONG_BREAK -> R.string.focus_phase_long
                    }), style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    Text(stringResource(R.string.focus_round, current.phase.round, current.config.rounds))
                    val seconds = (current.remainingMillis.coerceAtLeast(0) + 999) / 1000
                    // No live region on the ticking value: screen readers must not announce every second.
                    Text(stringResource(R.string.focus_remaining, seconds / 3600, seconds % 3600 / 60, seconds % 60),
                        style = MaterialTheme.typography.headlineMedium)
                }
                when (current.status) {
                    FocusStatus.RUNNING -> Button(enabled = enabled, onClick = { act { repository.pause(expectedGeneration = current.generation) } }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.focus_pause))
                    }
                    FocusStatus.PAUSED -> Button(enabled = enabled, onClick = { withNotificationPermission { act { repository.resume(expectedGeneration = current.generation) } } }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.focus_resume))
                    }
                    FocusStatus.INTERRUPTED -> Text(stringResource(R.string.focus_interrupted_note))
                    else -> Unit
                }
                if (current.status != FocusStatus.IDLE) OutlinedButton(enabled = enabled, onClick = { resetGeneration = current.generation; resetConfirm = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.focus_reset))
                }
            }
            if (failed) Text(stringResource(R.string.focus_failure), color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            if (!notificationsAllowed) Text(stringResource(R.string.focus_notifications_off))
            if (!exactAlarmsAllowed) Text(stringResource(R.string.focus_inexact_alerts))
            if (scheduleFailed) Text(stringResource(R.string.focus_schedule_failed), color = MaterialTheme.colorScheme.error)
            Text(stringResource(R.string.focus_notification_note), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.focus_background_note), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.focus_local_note), style = MaterialTheme.typography.bodySmall)
        })
    }
    replaceConfig?.let { replacement ->
        AlertDialog(onDismissRequest = { replaceConfig = null }, title = { Text(stringResource(R.string.focus_replace_title)) },
            text = { Text(stringResource(R.string.focus_replace_message)) },
            confirmButton = { TextButton(onClick = { replaceConfig = null; start(replacement.first, replacement.second, replacementApproved = true) }) { Text(stringResource(R.string.focus_replace)) } },
            dismissButton = { TextButton(onClick = { replaceConfig = null }) { Text(stringResource(R.string.focus_cancel)) } })
    }
    if (resetConfirm) AlertDialog(onDismissRequest = { resetConfirm = false }, title = { Text(stringResource(R.string.focus_reset_title)) },
        text = { Text(stringResource(R.string.focus_reset_message)) },
        confirmButton = { TextButton(onClick = { resetConfirm = false; val expected = resetGeneration; act { if (expected == null) repository.resetUnreadable() else repository.reset(expectedGeneration = expected) } }) { Text(stringResource(R.string.focus_reset)) } },
        dismissButton = { TextButton(onClick = { resetConfirm = false }) { Text(stringResource(R.string.focus_cancel)) } })
}

@Composable
private fun FocusNumberField(value: String, onChange: (String) -> Unit, label: Int, enabled: Boolean) {
    OutlinedTextField(value, { if (it.length <= 10) onChange(it) }, label = { Text(stringResource(label)) },
        enabled = enabled, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
}
