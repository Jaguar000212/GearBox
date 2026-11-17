package com.jaguar.gearbox.ui.components

import com.jaguar.gearbox.data.localizedInputLabel
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
import com.jaguar.gearbox.data.ToolMemoryPolicy
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
    // Automatic hydration is distinct from explicit History/Workbench input imports.
    onRestoreLastResult: (Map<String, String>) -> Unit = onRestore,
    // QR/Hash hydrate inputs only and cannot produce output until a user action.
    resultRequiresExplicitAction: Boolean = false,
    // Some editors restore a different result schema/mode than their initial screen state.
    restoredResultRequiresExplicitAction: (Map<String, String>) -> Boolean = { resultRequiresExplicitAction },
    // Opt-in semantic validator: rejected restores must not release dormant hydration capture.
    canResetHydratedCaptureOnRestore: (Map<String, String>) -> Boolean = { false },
    // Multi-schema editors can restore a different mode-key shape than their initial view.
    restoredModeInputs: (Map<String, String>) -> Map<String, String> = { saved ->
        modeInputs.mapValues { (key, value) -> saved[key] ?: value }
    },
): ToolHistoryBinding {
    val context = LocalContext.current
    val store = remember { ToolMemoryStore(context) }
    val restore by rememberUpdatedState(onRestore)
    val restoreMode by rememberUpdatedState(onRestoreMode)
    val restoreLast by rememberUpdatedState(onRestoreLastResult)
    val restoreModeInputs by rememberUpdatedState(restoredModeInputs)
    val canResetHydratedCapture by rememberUpdatedState(canResetHydratedCaptureOnRestore)
    var importedInputsFingerprint by rememberSaveable(route) { mutableStateOf<String?>(null) }
    var hydratedInputsFingerprint by rememberSaveable(route) { mutableStateOf<String?>(null) }
    var hydratedModeFingerprint by rememberSaveable(route) { mutableStateOf<String?>(null) }
    val latestInputs by rememberUpdatedState(inputs)
    val initialInputs = remember(route) { inputs }
    val imported = LocalToolImport.current
    val consumeImport = LocalConsumeToolImport.current
    var initialized by rememberSaveable(route) { mutableStateOf(false) }
    // A new explicit import wins even when Navigation restored an already initialized draft.
    // The import ID also cancels an older asynchronous preference load before it can overwrite it.
    LaunchedEffect(route, imported?.id) {
        if (imported?.route == route) {
            if (com.jaguar.gearbox.data.ToolMemoryPolicy.permitsRoute(route) &&
                com.jaguar.gearbox.data.ToolMemoryPolicy.validInputs(imported.inputs)) {
                importedInputsFingerprint = if (imported.suppressInitialRetention)
                    historyObservationFingerprint(route, com.jaguar.gearbox.data.retentionInputValues(inputs + imported.inputs), null) else null
                if (imported.suppressInitialRetention) {
                    hydratedModeFingerprint = historyObservationFingerprint(route,
                        modeInputs.mapValues { (key, value) -> imported.inputs[key] ?: value }, null)
                }
                if (imported.modeOnly) restoreMode(imported.inputs) else {
                    if (canResetHydratedCapture(imported.inputs)) hydratedInputsFingerprint = null
                    restore(imported.inputs)
                }
            }
            consumeImport()
            initialized = true
            return@LaunchedEffect
        }
        if (initialized) return@LaunchedEffect
        val saved = withContext(Dispatchers.IO) { store.loadMode(route) to if (restoreLastResult) store.loadResult(route) else null }
        if (latestInputs == initialInputs) {
            if (saved.first.isNotEmpty()) restoreMode(saved.first)
            saved.second?.let {
                // Loading a saved result is not a new run. In particular, opening a tool
                // after clearing its history must not copy its dormant last result back.
                hydratedInputsFingerprint = if (restoredResultRequiresExplicitAction(it.inputs)) null
                    else historyObservationFingerprint(route, it.inputs, null)
                hydratedModeFingerprint = historyObservationFingerprint(route, restoreModeInputs(it.inputs), null)
                restoreLast(it.inputs)
            }
        }
        initialized = true
    }
    // Remember attempted mode observations too: clearing modes must not be undone
    // merely by recreating the screen with the same configuration still displayed.
    var observedModeFingerprint by rememberSaveable(route) { mutableStateOf<String?>(null) }
    var observedModeGeneration by rememberSaveable(route) { mutableStateOf(0L) }
    var modeObservationProcessed by rememberSaveable(route) { mutableStateOf(false) }
    val modeFingerprint = remember(route, modeInputs) { historyObservationFingerprint(route, modeInputs, null) }
    LaunchedEffect(route, modeFingerprint, initialized, imported?.id) {
        if (imported?.route == route) return@LaunchedEffect
        if (!initialized) return@LaunchedEffect
        if (hydratedModeFingerprint == modeFingerprint) return@LaunchedEffect
        hydratedModeFingerprint = null
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
    LaunchedEffect(route, fingerprint, initialized, imported?.id) {
        // Cancel any old debounce immediately, before restore updates the screen's inputs.
        if (imported?.route == route) return@LaunchedEffect
        val currentInputsFingerprint = historyObservationFingerprint(route,
            com.jaguar.gearbox.data.retentionInputValues(inputs), null)
        if (com.jaguar.gearbox.data.suppressImportedObservation(importedInputsFingerprint,
                currentInputsFingerprint, resultRequiresExplicitAction, !output.isNullOrBlank())) return@LaunchedEffect
        importedInputsFingerprint = null
        // Async results may temporarily disappear during recreation. Loading/invalid
        // output must not erase the processed observation and replay it on completion.
        if (!initialized || output.isNullOrBlank()) return@LaunchedEffect
        if (hydratedInputsFingerprint == historyObservationFingerprint(route, inputs, null)) return@LaunchedEffect
        hydratedInputsFingerprint = null
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
    return ToolHistoryBinding(route, {
        // Explicit History restore is a fresh user action, unlike automatic hydration.
        if (canResetHydratedCapture(it)) hydratedInputsFingerprint = null
        restore(it)
    }, inputs, output, modeInputs, allowRestore)
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
    var sharePreview by remember { mutableStateOf<String?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(binding.route, binding.inputs, binding.output) {
        delay(1100)
        withContext(Dispatchers.IO) { store.history(binding.route) to store.settings().permitsHistory(binding.route) }.let {
            records = it.first; enabled = it.second; loaded = true
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(context.getString(com.jaguar.gearbox.R.string.shared_tool_history)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row {
                    TextButton(enabled = binding.configuration.isNotEmpty(), onClick = { saving = "preset"; name = "" }) { Text(context.getString(com.jaguar.gearbox.R.string.shared_save_setup)) }
                    if (!binding.output.isNullOrBlank()) TextButton(onClick = { saving = "result"; name = "" }) { Text(context.getString(com.jaguar.gearbox.R.string.shared_keep_result)) }
                }
                if (binding.route == com.jaguar.gearbox.data.Tools.ROUTE_TEXT_TOOLS && !binding.output.isNullOrBlank() && handoff != null) {
                    Row {
                        TextButton(onClick = { onDismiss(); handoff(com.jaguar.gearbox.data.Tools.ROUTE_QR_GENERATOR, mapOf("text" to binding.output.orEmpty())) }) { Text(context.getString(com.jaguar.gearbox.R.string.shared_use_in_qr)) }
                        TextButton(onClick = { onDismiss(); handoff(com.jaguar.gearbox.data.Tools.ROUTE_HASH_GENERATOR, mapOf("text" to binding.output.orEmpty())) }) { Text(context.getString(com.jaguar.gearbox.R.string.shared_use_in_hash)) }
                    }
                }
                if (!enabled) Text(context.getString(com.jaguar.gearbox.R.string.shared_automatic_history_is_off_for_this_tool_review_global_and_per_tool_cont))
                Text(context.getString(com.jaguar.gearbox.R.string.shared_large_entries_are_not_recorded_each_field_and_result_is_limited_to_4_0), style = MaterialTheme.typography.bodySmall)
                if (!loaded) LinearProgressIndicator(Modifier.fillMaxWidth())
                else if (records.isEmpty()) Text(context.getString(com.jaguar.gearbox.R.string.shared_no_saved_runs_yet_valid_results_are_recorded_after_you_pause_editing_w))
                LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(records, key = { it.id }) { entry ->
                        OutlinedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(entry.timestampMillis)), style = MaterialTheme.typography.labelSmall)
                                Text(entry.inputs.filterKeys { !it.startsWith("_") }.entries.joinToString("\n") { context.getString(com.jaguar.gearbox.R.string.shared_label_value, localizedInputLabel(context, it.key), it.value) }, style = MaterialTheme.typography.bodySmall)
                                Text(entry.output, style = MaterialTheme.typography.bodyLarge)
                                Row {
                                    TextButton(onClick = { context.copyToClipboard(context.getString(com.jaguar.gearbox.R.string.shared_tool_history), historyText(context, entry), sensitive = ToolMemoryPolicy.isSensitiveTextRoute(binding.route)) }) { Text(context.getString(com.jaguar.gearbox.R.string.shared_copy)) }
                                    TextButton(onClick = { if (ToolMemoryPolicy.isSensitiveTextRoute(binding.route)) sharePreview = historyText(context, entry) else context.shareText(historyText(context, entry)) }) { Text(context.getString(com.jaguar.gearbox.R.string.shared_share)) }
                                    if (binding.allowRestore) TextButton(onClick = { binding.restore(entry.inputs); onDismiss() }) { Text(context.getString(com.jaguar.gearbox.R.string.shared_restore)) }
                                }
                                TextButton(onClick = { scope.launch {
                                    val success = withContext(Dispatchers.IO) { store.remove(binding.route, entry.id) }
                                    if (success) records = records.filterNot { it.id == entry.id } else error = context.getString(com.jaguar.gearbox.R.string.shared_could_not_delete_this_entry_try_again)
                                } }) { Text(context.getString(com.jaguar.gearbox.R.string.shared_delete)) }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(context.getString(com.jaguar.gearbox.R.string.shared_close)) } },
        dismissButton = { if (records.isNotEmpty()) TextButton(onClick = { confirmClear = true }) { Text(context.getString(com.jaguar.gearbox.R.string.shared_clear_history)) } },
    )
    sharePreview?.let { text ->
        SensitiveTextShareDialog(text, onConfirm = {
            sharePreview = null
            runCatching { context.shareText(text) }.onFailure { error = context.getString(com.jaguar.gearbox.R.string.shared_no_sharing_app_is_available_you_can_copy_this_entry_instead) }
        }, onDismiss = { sharePreview = null })
    }
    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false }, title = { Text(context.getString(com.jaguar.gearbox.R.string.shared_clear_this_tool_s_history)) },
        text = { Text(context.getString(com.jaguar.gearbox.R.string.shared_this_removes_saved_inputs_and_results_from_this_device_it_cannot_be_un)) },
        confirmButton = { TextButton(onClick = { scope.launch {
            val success = withContext(Dispatchers.IO) { store.clearHistory(binding.route) }
            if (success) records = emptyList() else error = context.getString(com.jaguar.gearbox.R.string.shared_could_not_clear_history_try_again)
            confirmClear = false
        } }) { Text(context.getString(com.jaguar.gearbox.R.string.shared_clear)) } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(context.getString(com.jaguar.gearbox.R.string.shared_cancel)) } },
    )
    if (saving != null) AlertDialog(onDismissRequest = { if (!busy) saving = null },
        title = { Text(if (saving == "preset") context.getString(com.jaguar.gearbox.R.string.shared_save_setup) else context.getString(com.jaguar.gearbox.R.string.shared_keep_result)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(context.getString(com.jaguar.gearbox.R.string.shared_saved_only_on_this_device_in_your_workbench_review_inputs_before_savin))
            if (ToolMemoryPolicy.isSensitiveTextRoute(binding.route)) {
                Text(context.getString(com.jaguar.gearbox.R.string.shared_text_qr_and_hash_may_contain_passwords_tokens_or_private_information_t))
                Text(context.getString(com.jaguar.gearbox.R.string.shared_save_preview), style = MaterialTheme.typography.titleSmall)
                val previewInputs = if (saving == "preset") binding.configuration else binding.inputs
                Text(previewInputs.filterKeys { !it.startsWith("_") }.entries.joinToString("\n") { context.getString(com.jaguar.gearbox.R.string.shared_label_value, localizedInputLabel(context, it.key), it.value) })
                if (saving != "preset" && !binding.output.isNullOrBlank()) Text(binding.output.orEmpty())
            }
            OutlinedTextField(value = name, onValueChange = { name = it.take(80) }, label = { Text(context.getString(com.jaguar.gearbox.R.string.shared_name)) })
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
                    if (!success) error = context.getString(com.jaguar.gearbox.R.string.shared_could_not_save_check_the_100_item_limit_and_input_size)
                    saving = null
                    busy = false
                }
            }
        }) { Text(context.getString(com.jaguar.gearbox.R.string.shared_save)) } },
        dismissButton = { TextButton(enabled = !busy, onClick = { saving = null }) { Text(context.getString(com.jaguar.gearbox.R.string.shared_cancel)) } },
    )

}

private fun historyText(context: android.content.Context, entry: ToolHistoryEntry) = entry.inputs.filterKeys { !it.startsWith("_") }.entries.joinToString("\n") { context.getString(com.jaguar.gearbox.R.string.shared_label_value, localizedInputLabel(context, it.key), it.value) } + "\n\n" + entry.output

data class ToolImport(val route: String, val inputs: Map<String, String>, val modeOnly: Boolean = false,
    val id: String = java.util.UUID.randomUUID().toString(),
    val suppressInitialRetention: Boolean = false)
val LocalToolImport = staticCompositionLocalOf<ToolImport?> { null }
val LocalConsumeToolImport = staticCompositionLocalOf<() -> Unit> { {} }

val LocalToolPresetOpener = staticCompositionLocalOf<((Map<String, String>) -> Unit)?> { null }

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
