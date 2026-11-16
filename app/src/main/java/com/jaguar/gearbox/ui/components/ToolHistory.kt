package com.jaguar.gearbox.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.ToolHistoryEntry
import com.jaguar.gearbox.data.ToolMemoryStore
import com.jaguar.gearbox.ui.tools.copyToClipboard
import com.jaguar.gearbox.ui.tools.shareText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.text.DateFormat
import java.util.Date

/** A tool explicitly declares safe serializable inputs and a validated restore operation. */
class ToolHistoryBinding(
    val route: String,
    val restore: (Map<String, String>) -> Unit,
    val inputs: Map<String, String> = emptyMap(),
    val output: String? = null,
    val configuration: Map<String, String> = emptyMap(),
    val allowRestore: Boolean = true,
)

@Composable
fun rememberToolHistory(
    route: String,
    inputs: Map<String, String>,
    output: String?,
    onRestore: (Map<String, String>) -> Unit,
    modeInputs: Map<String, String> = emptyMap(),
    onRestoreMode: (Map<String, String>) -> Unit = {},
    recordingDelayMillis: Long = 1000L,
    restoreLastResult: Boolean = true,
    allowRestore: Boolean = true,
    expectedHistoryGeneration: Long? = null,
): ToolHistoryBinding {
    val context = LocalContext.current
    val store = remember { ToolMemoryStore(context) }
    val restore by rememberUpdatedState(onRestore)
    val restoreMode by rememberUpdatedState(onRestoreMode)
    val latestInputs by rememberUpdatedState(inputs)
    val initialInputs = remember(route) { inputs }
    val imported = LocalToolImport.current
    val consumeImport = LocalConsumeToolImport.current
    var initialized by rememberSaveable(route) { mutableStateOf(false) }
    LaunchedEffect(route) {
        if (initialized) return@LaunchedEffect
        if (imported?.route == route) {
            if (imported.modeOnly) restoreMode(imported.inputs) else restore(imported.inputs)
            consumeImport()
            initialized = true
            return@LaunchedEffect
        }
        val saved = withContext(Dispatchers.IO) { store.loadMode(route) to if (restoreLastResult) store.loadResult(route) else null }
        if (latestInputs == initialInputs) {
            if (saved.first.isNotEmpty()) restoreMode(saved.first)
            saved.second?.let { restore(it.inputs) }
        }
        initialized = true
    }
    // Remember attempted mode observations too: clearing modes must not be undone
    // merely by recreating the screen with the same configuration still displayed.
    var observedModeFingerprint by rememberSaveable(route) { mutableStateOf<String?>(null) }
    var observedModeGeneration by rememberSaveable(route) { mutableStateOf(0L) }
    var modeObservationProcessed by rememberSaveable(route) { mutableStateOf(false) }
    val modeFingerprint = remember(route, modeInputs) { historyObservationFingerprint(route, modeInputs, null) }
    LaunchedEffect(route, modeFingerprint, initialized) {
        if (!initialized) return@LaunchedEffect
        if (observedModeFingerprint != modeFingerprint) {
            observedModeFingerprint = modeFingerprint
            observedModeGeneration = store.modeGeneration(route)
            modeObservationProcessed = false
        }
        if (!modeObservationProcessed && modeInputs.isNotEmpty()) {
            val token = observedModeGeneration
            modeObservationProcessed = true
            withContext(Dispatchers.IO) { store.saveMode(route, modeInputs, expectedGeneration = token) }
        }
    }
    // Keep only a bounded fingerprint, never a second saved-state copy of private inputs.
    // The generation belongs to the observation, not the recreated coroutine: a clear
    // during debounce must remain authoritative even if the device rotates afterward.
    var observedFingerprint by rememberSaveable(route) { mutableStateOf<String?>(null) }
    var observedGeneration by rememberSaveable(route) { mutableStateOf(0L) }
    var observedResultGeneration by rememberSaveable(route) { mutableStateOf(0L) }
    var observationProcessed by rememberSaveable(route) { mutableStateOf(false) }
    val fingerprint = remember(route, inputs, output) { historyObservationFingerprint(route, inputs, output) }
    // Live calculators are recorded after a pause, never for each keystroke or invalid result.
    LaunchedEffect(route, fingerprint, initialized) {
        if (!initialized) return@LaunchedEffect
        if (observedFingerprint != fingerprint) {
            observedFingerprint = fingerprint
            observedGeneration = expectedHistoryGeneration ?: store.historyGeneration(route)
            observedResultGeneration = store.resultGeneration(route)
            observationProcessed = false
        }
        if (!observationProcessed && !output.isNullOrBlank()) {
            val generation = observedGeneration
            val resultToken = observedResultGeneration
            delay(recordingDelayMillis.coerceAtLeast(0L))
            // Mark before dispatch: recreation must not replay an attempted write, even
            // when settings or a concurrent clear deliberately reject that write.
            observationProcessed = true
            withContext(Dispatchers.IO) {
                store.record(route, inputs, output, expectedGeneration = generation)
                if (restoreLastResult) store.saveResult(route, inputs, output, expectedGeneration = resultToken)
            }
        }
    }
    return ToolHistoryBinding(route, { restore(it) }, inputs, output, modeInputs, allowRestore)
}

@Composable
fun ToolHistoryDialog(binding: ToolHistoryBinding, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val store = remember { ToolMemoryStore(context) }
    val scope = rememberCoroutineScope()
    val handoff = LocalToolHandoff.current
    var records by remember { mutableStateOf<List<ToolHistoryEntry>>(emptyList()) }
    var enabled by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(binding.route, binding.inputs, binding.output) {
        delay(1100)
        withContext(Dispatchers.IO) { store.history(binding.route) to store.settings().historyEnabled }.let {
            records = it.first; enabled = it.second; loaded = true
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tool history") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row {
                    TextButton(enabled = binding.configuration.isNotEmpty(), onClick = { saving = "preset"; name = "" }) { Text("Save setup") }
                    if (!binding.output.isNullOrBlank()) TextButton(onClick = { saving = "result"; name = "" }) { Text("Keep result") }
                }
                if (binding.route == com.jaguar.gearbox.data.Tools.ROUTE_TEXT_TOOLS && !binding.output.isNullOrBlank() && handoff != null) {
                    Row {
                        TextButton(onClick = { onDismiss(); handoff(com.jaguar.gearbox.data.Tools.ROUTE_QR_GENERATOR, mapOf("text" to binding.output.orEmpty())) }) { Text("Use in QR") }
                        TextButton(onClick = { onDismiss(); handoff(com.jaguar.gearbox.data.Tools.ROUTE_HASH_GENERATOR, mapOf("text" to binding.output.orEmpty())) }) { Text("Use in Hash") }
                    }
                }
                if (!enabled) Text("History is off. Enable it and choose a limit in Settings → Tool memory. Existing entries stay until you delete them.")
                Text("Large entries are not recorded: each field and result is limited to 4,096 characters, with 8,192 total per entry.", style = MaterialTheme.typography.bodySmall)
                if (!loaded) LinearProgressIndicator(Modifier.fillMaxWidth())
                else if (records.isEmpty()) Text("No saved runs yet. Valid results are recorded after you pause editing when history is enabled.")
                LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(records, key = { it.id }) { entry ->
                        OutlinedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(entry.timestampMillis)), style = MaterialTheme.typography.labelSmall)
                                Text(entry.inputs.filterKeys { !it.startsWith("_") }.entries.joinToString("\n") { "${it.key}: ${it.value}" }, style = MaterialTheme.typography.bodySmall)
                                Text(entry.output, style = MaterialTheme.typography.bodyLarge)
                                Row {
                                    TextButton(onClick = { context.copyToClipboard("Tool history", historyText(entry)) }) { Text("Copy") }
                                    TextButton(onClick = { context.shareText(historyText(entry)) }) { Text("Share") }
                                    if (binding.allowRestore) TextButton(onClick = { binding.restore(entry.inputs); onDismiss() }) { Text("Restore") }
                                }
                                TextButton(onClick = { scope.launch {
                                    val success = withContext(Dispatchers.IO) { store.remove(binding.route, entry.id) }
                                    if (success) records = records.filterNot { it.id == entry.id } else error = "Could not delete this entry. Try again."
                                } }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        dismissButton = { if (records.isNotEmpty()) TextButton(onClick = { confirmClear = true }) { Text("Clear history") } },
    )
    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false }, title = { Text("Clear this tool’s history?") },
        text = { Text("This removes saved inputs and results from this device. It cannot be undone.") },
        confirmButton = { TextButton(onClick = { scope.launch {
            val success = withContext(Dispatchers.IO) { store.clearHistory(binding.route) }
            if (success) records = emptyList() else error = "Could not clear history. Try again."
            confirmClear = false
        } }) { Text("Clear") } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
    )
    if (saving != null) AlertDialog(onDismissRequest = { if (!busy) saving = null },
        title = { Text(if (saving == "preset") "Save setup" else "Keep result") },
        text = { Column {
            Text("Saved only on this device in your Workbench. Review inputs before saving private information.")
            OutlinedTextField(value = name, onValueChange = { name = it.take(80) }, label = { Text("Name") })
        } },
        confirmButton = { TextButton(enabled = name.isNotBlank() && !busy, onClick = {
            if (!busy) {
                busy = true
                val saveKind = saving
                val savedName = name.trim()
                val snapshot = binding
                scope.launch {
                    val success = withContext(Dispatchers.IO) {
                        if (saveKind == "preset") store.savePreset(savedName, snapshot.route, snapshot.configuration)
                        else store.saveToWorkbench(savedName, snapshot.route, snapshot.inputs, snapshot.output.orEmpty())
                    }
                    if (!success) error = "Could not save. Check the 100-item limit and input size."
                    saving = null
                    busy = false
                }
            }
        }) { Text("Save") } },
        dismissButton = { TextButton(enabled = !busy, onClick = { saving = null }) { Text("Cancel") } },
    )

}

private fun historyText(entry: ToolHistoryEntry) = entry.inputs.filterKeys { !it.startsWith("_") }.entries.joinToString("\n") { "${it.key}: ${it.value}" } + "\n\n" + entry.output

data class ToolImport(val route: String, val inputs: Map<String, String>, val modeOnly: Boolean = false)
val LocalToolImport = staticCompositionLocalOf<ToolImport?> { null }
val LocalConsumeToolImport = staticCompositionLocalOf<() -> Unit> { {} }

val LocalToolHandoff = staticCompositionLocalOf<((String, Map<String, String>) -> Unit)?> { null }

/** Length framing and sorted keys avoid ambiguous map/string concatenations. Event keys
 * such as _run and _capture intentionally participate so explicit repeats remain runs. */
private fun historyObservationFingerprint(route: String, inputs: Map<String, String>, output: String?): String {
    val digest = MessageDigest.getInstance("SHA-256")
    fun add(value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        val size = bytes.size
        digest.update(byteArrayOf((size ushr 24).toByte(), (size ushr 16).toByte(), (size ushr 8).toByte(), size.toByte()))
        digest.update(bytes)
    }
    add(route)
    add(inputs.size.toString())
    inputs.toSortedMap().forEach { (key, value) -> add(key); add(value) }
    add(if (output == null) "null" else "value")
    add(output.orEmpty())
    return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
