package com.jaguar.gearbox.ui.tools

import com.jaguar.gearbox.R

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.ToolMemoryStore
import com.jaguar.gearbox.data.SimplePrefsStore
import com.jaguar.gearbox.data.localizedSummary
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.IntListSaver
import com.jaguar.gearbox.ui.components.ToolActions
import com.jaguar.gearbox.ui.components.ToolScaffold
import com.jaguar.gearbox.ui.components.rememberToolHistory
import com.jaguar.gearbox.logic.TambolaSnapshot
import com.jaguar.gearbox.logic.parseTambolaSnapshot

private const val KEY_CALLED = "tambola.called"
private const val KEY_LAST_CALLED = "tambola.last_called"

@Composable
fun TambolaScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { SimplePrefsStore(context, gameSession = true) }
    val memory = remember { ToolMemoryStore(context) }
    var calledNumbers by rememberSaveable(stateSaver = IntListSaver) {
        mutableStateOf(store.getIntList(KEY_CALLED))
    }
    var lastCalled by rememberSaveable { mutableStateOf(store.getInt(KEY_LAST_CALLED, 0)) }
    // Capture is a one-shot UI event, not restorable state: rotation must not re-record a deleted snapshot.
    var capturedCalls by remember { mutableStateOf<String?>(null) }
    var captureId by remember { mutableStateOf("") }
    var captureGeneration by remember { mutableStateOf<Long?>(null) }
    var pendingRestore by rememberSaveable { mutableStateOf<String?>(null) }
    var importError by rememberSaveable { mutableStateOf<String?>(null) }
    var showResetConfirm by rememberSaveable { mutableStateOf(false) }

    fun persistGame(numbers: List<Int>, last: Int) {
        store.edit {
            putString(KEY_CALLED, numbers.joinToString(","))
            putInt(KEY_LAST_CALLED, last)
        }
    }

    fun applySnapshot(snapshot: TambolaSnapshot) {
        calledNumbers = snapshot.calls
        lastCalled = snapshot.last
        persistGame(snapshot.calls, snapshot.last)
        capturedCalls = null
        importError = null
    }
    val captured = capturedCalls?.let { parseTambolaSnapshot(mapOf("calls" to it)) }
    val history = rememberToolHistory(
        route = Tools.ROUTE_TAMBOLA,
        inputs = captured?.inputs()?.plus("_run" to captureId) ?: emptyMap(),
        output = captured?.localizedSummary(context),
        recordingDelayMillis = 0L,
        expectedHistoryGeneration = captureGeneration,
        restoreLastResult = false,
        onRestore = { saved ->
            val snapshot = parseTambolaSnapshot(saved)
            if (snapshot == null) importError = context.getString(R.string.game_this_snapshot_contains_invalid_or_repeated_numbers_the_cur)
            else if (calledNumbers.isNotEmpty()) pendingRestore = snapshot.inputs().getValue("calls")
            else applySnapshot(snapshot)
        },
    )
    if (pendingRestore != null) AlertDialog(
        onDismissRequest = { pendingRestore = null },
        title = { Text(context.getString(R.string.game_restore_tambola_snapshot)) },
        text = { Text(context.getString(R.string.game_this_replaces_the_current_1_s_called_numbers_with_the_save, calledNumbers.size)) },
        confirmButton = { TextButton(onClick = {
            pendingRestore?.let { parseTambolaSnapshot(mapOf("calls" to it)) }?.let { applySnapshot(it) }
            pendingRestore = null
        }) { Text(context.getString(R.string.game_restore)) } },
        dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text(context.getString(R.string.game_keep_current_game)) } },
    )

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(context.getString(R.string.game_reset_game)) },
            text = { Text(context.getString(R.string.game_this_clears_all_1_s_called_numbers_this_can_t_be_undone, calledNumbers.size)) },
            confirmButton = {
                TextButton(onClick = {
                    calledNumbers = emptyList()
                    lastCalled = 0
                    persistGame(calledNumbers, lastCalled)
                    showResetConfirm = false
                }) { Text(context.getString(R.string.game_reset_45)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text(context.getString(R.string.game_cancel)) }
            },
        )
    }

    ToolScaffold(
        title = context.getString(R.string.game_tambola_numbers),
        icon = Tools.byRoute(Tools.ROUTE_TAMBOLA)!!.icon,
        onNavigateBack = onNavigateBack,
        history = history,
    ) {
        importError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        val remaining = (1..90).filter { it !in calledNumbers }
        Card(
            modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (lastCalled == 0) context.getString(R.string.game_ready_to_play) else context.getString(R.string.game_current_number), style = MaterialTheme.typography.titleMedium)
                Text(
                    if (lastCalled == 0) "—" else lastCalled.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                Text(
                    if (remaining.isEmpty()) context.getString(R.string.game_all_90_numbers_called_game_complete)
                    else context.getString(R.string.game_1_s_called_2_s_remaining, calledNumbers.size, remaining.size),
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(context.getString(R.string.game_call_numbers_from_1_to_90_without_repeats), style = MaterialTheme.typography.bodyMedium)
        if (calledNumbers.size > 1) {
            Spacer(Modifier.height(12.dp))
            Text(context.getString(R.string.game_previous_calls_newest_first), style = MaterialTheme.typography.labelLarge)
            Text(calledNumbers.reversed().drop(1).take(5).joinToString("   ·   "),
                style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                // Read current state again: repeated taps may arrive before recomposition.
                val available = (1..90).filter { it !in calledNumbers }
                if (available.isNotEmpty()) {
                    val next = available.random()
                    calledNumbers = calledNumbers + next
                    lastCalled = next
                    persistGame(calledNumbers, lastCalled)
                }
            },
            enabled = remaining.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Casino, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (remaining.isEmpty()) context.getString(R.string.game_game_complete) else context.getString(R.string.game_call_number))
        }
        OutlinedButton(
            onClick = { showResetConfirm = true },
            enabled = calledNumbers.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(context.getString(R.string.game_reset_game_202))
        }
        OutlinedButton(onClick = {
            // Fence the explicit event before dispatching its recording effect.
            captureGeneration = memory.historyGeneration(Tools.ROUTE_TAMBOLA)
            capturedCalls = calledNumbers.joinToString(",")
            captureId = java.util.UUID.randomUUID().toString()
        }, enabled = calledNumbers.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text(context.getString(R.string.game_capture_snapshot)) }
        Text(if (captured == null) context.getString(R.string.game_capture_this_game_for_history_recording_follows_your_histo)
            else context.getString(R.string.game_snapshot_prepared_1_s_calls_open_history_to_copy_share_or, captured.calls.size),
            style = MaterialTheme.typography.bodySmall)
        val summary = context.getString(R.string.game_tambola_1_s_of_90_called_n, calledNumbers.size) +
            context.getString(R.string.game_current_1_s_ncalled_in_order_2_s, lastCalled, calledNumbers.joinToString(", "))
        ToolActions(
            onCopy = { context.copyToClipboard(context.getString(R.string.game_tambola), summary) },
            onShare = { context.shareText(summary) },
            resultEnabled = calledNumbers.isNotEmpty(),
        )
        Spacer(Modifier.height(20.dp))
        Text(context.getString(R.string.game_number_board), style = MaterialTheme.typography.titleMedium)
        Text(context.getString(R.string.game_called_outlined_current), style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        // One parent scroll surface: all 90 numbers remain reachable at large font sizes.
        val fontScale = LocalDensity.current.fontScale
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val columns = if (maxWidth >= 600.dp && fontScale <= 1.3f) 10 else 5
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..90).chunked(columns).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { number ->
                            val called = number in calledNumbers
                            val current = number == lastCalled
                            Card(
                                modifier = Modifier.weight(1f).clearAndSetSemantics {
                                    contentDescription = "$number, " + when {
                                        current -> context.getString(R.string.game_current_number_called)
                                        called -> context.getString(R.string.game_called)
                                        else -> context.getString(R.string.game_not_called)
                                    }
                                },
                                border = if (current) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                colors = CardDefaults.cardColors(containerColor = if (called)
                                    MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                            ) {
                                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(number.toString(), style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (current) FontWeight.Bold else FontWeight.Normal)
                                    Text(if (called) "✓" else "·", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
