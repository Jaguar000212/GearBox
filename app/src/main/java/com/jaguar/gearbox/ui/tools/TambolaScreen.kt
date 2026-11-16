package com.jaguar.gearbox.ui.tools

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
        output = captured?.summary(),
        recordingDelayMillis = 0L,
        expectedHistoryGeneration = captureGeneration,
        restoreLastResult = false,
        onRestore = { saved ->
            val snapshot = parseTambolaSnapshot(saved)
            if (snapshot == null) importError = "This snapshot contains invalid or repeated numbers. The current game was not changed."
            else if (calledNumbers.isNotEmpty()) pendingRestore = snapshot.inputs().getValue("calls")
            else applySnapshot(snapshot)
        },
    )
    if (pendingRestore != null) AlertDialog(
        onDismissRequest = { pendingRestore = null },
        title = { Text("Restore Tambola snapshot?") },
        text = { Text("This replaces the current ${calledNumbers.size} called numbers with the saved game. No new number will be called.") },
        confirmButton = { TextButton(onClick = {
            pendingRestore?.let { parseTambolaSnapshot(mapOf("calls" to it)) }?.let { applySnapshot(it) }
            pendingRestore = null
        }) { Text("Restore") } },
        dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("Keep current game") } },
    )

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset game?") },
            text = { Text("This clears all ${calledNumbers.size} called numbers. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    calledNumbers = emptyList()
                    lastCalled = 0
                    persistGame(calledNumbers, lastCalled)
                    showResetConfirm = false
                }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            },
        )
    }

    ToolScaffold(
        title = "Tambola Numbers",
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
                Text(if (lastCalled == 0) "Ready to play" else "Current number", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (lastCalled == 0) "—" else lastCalled.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                Text(
                    if (remaining.isEmpty()) "All 90 numbers called. Game complete!"
                    else "${calledNumbers.size} called • ${remaining.size} remaining",
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Call numbers from 1 to 90, without repeats.", style = MaterialTheme.typography.bodyMedium)
        if (calledNumbers.size > 1) {
            Spacer(Modifier.height(12.dp))
            Text("Previous calls · newest first", style = MaterialTheme.typography.labelLarge)
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
            Text(if (remaining.isEmpty()) "Game complete" else "Call number")
        }
        OutlinedButton(
            onClick = { showResetConfirm = true },
            enabled = calledNumbers.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Reset game")
        }
        OutlinedButton(onClick = {
            // Fence the explicit event before dispatching its recording effect.
            captureGeneration = memory.historyGeneration(Tools.ROUTE_TAMBOLA)
            capturedCalls = calledNumbers.joinToString(",")
            captureId = java.util.UUID.randomUUID().toString()
        }, enabled = calledNumbers.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text("Capture snapshot") }
        Text(if (captured == null) "Capture this game for History. Recording follows your history setting."
            else "Snapshot prepared: ${captured.calls.size} calls. Open History to copy, share or Keep result; automatic recording follows Settings.",
            style = MaterialTheme.typography.bodySmall)
        val summary = "Tambola: ${calledNumbers.size} of 90 called\n" +
            "Current: $lastCalled\nCalled in order: ${calledNumbers.joinToString(", ")}"
        ToolActions(
            onCopy = { context.copyToClipboard("Tambola", summary) },
            onShare = { context.shareText(summary) },
            resultEnabled = calledNumbers.isNotEmpty(),
        )
        Spacer(Modifier.height(20.dp))
        Text("Number board", style = MaterialTheme.typography.titleMedium)
        Text("✓ Called · outlined = current", style = MaterialTheme.typography.bodySmall)
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
                                        current -> "current number, called"
                                        called -> "called"
                                        else -> "not called"
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
