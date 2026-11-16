package com.jaguar.gearbox.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.GameSessionPolicy
import com.jaguar.gearbox.data.ToolMemorySettings
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.data.ToolHistoryEntry
import com.jaguar.gearbox.data.ToolMemoryStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val privateInputTools = listOf(
    Tools.ROUTE_TEXT_TOOLS to "Text Tools",
    Tools.ROUTE_QR_GENERATOR to "QR Generator",
    Tools.ROUTE_HASH_GENERATOR to "Hash Generator",
)

/** Local retention choices are explicit and separate from recent-tool navigation. */
@Composable
internal fun ToolMemorySettingsSection() {
    val context = LocalContext.current
    val store = remember(context) { ToolMemoryStore(context) }
    val sessions = remember(context) { GameSessionPolicy(context, store) }
    var settings by remember { mutableStateOf(ToolMemorySettings()) }
    var busy by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    var open by rememberSaveable { mutableStateOf(false) }
    var clearTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var limitText by rememberSaveable { mutableStateOf(settings.historyLimit.toString()) }
    var feedback by rememberSaveable { mutableStateOf<String?>(null) }
    var savedCounts by remember { mutableStateOf<Map<String, Pair<Int, Int>>>(emptyMap()) }
    var reviewTitle by remember { mutableStateOf<String?>(null) }
    var reviewEntries by remember { mutableStateOf<List<Pair<String, ToolHistoryEntry>>>(emptyList()) }
    suspend fun refreshCounts() {
        savedCounts = withContext(Dispatchers.IO) {
            privateInputTools.associate { (route, _) ->
                route to (store.history(route).size to if (store.rememberedResult(route) != null) 1 else 0)
            }
        }
    }
    LaunchedEffect(store) {
        settings = withContext(Dispatchers.IO) { store.settings() }
        limitText = settings.historyLimit.toString()
        refreshCounts()
        busy = false
    }
    fun save(next: ToolMemorySettings) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                if (withContext(Dispatchers.IO) { store.saveSettings(next) }) {
                    settings = withContext(Dispatchers.IO) { store.settings() }; feedback = null
                    refreshCounts()
                } else feedback = "Could not save preferences. Please try again."
            } finally { busy = false }
        }
    }

    Text("Local data", style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp))
    ListItem(
        headlineContent = { Text("Tool memory & history") },
        supportingContent = { Text(if (settings.historyEnabled)
            "History on · Up to ${settings.historyLimit} entries per tool"
            else "History off · Choose what your tools remember") },
        modifier = Modifier.clickable(enabled = !busy) { limitText = settings.historyLimit.toString(); open = true },
    )
    if (open) {
        AlertDialog(
            onDismissRequest = { if (!busy) open = false },
            title = { Text("Tool memory & history") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("New history and remembered modes/results stay in app-private storage on this device, outside cloud backup. This is not an encrypted vault. Password-generator secrets are never saved in history.",
                        style = MaterialTheme.typography.bodyMedium)
                    MemoryToggle("Remember tool modes", "Reuse supported tools’ last selected options.",
                        settings.rememberModes, !busy) { save(settings.copy(rememberModes = it)) }
                    MemoryToggle("Remember last result", "Recalculate supported tools from saved inputs; random tools restore their last outcome. QR and Hash still require Generate. Separate from history.",
                        settings.rememberResults, !busy) { save(settings.copy(rememberResults = it)) }
                    HorizontalDivider()
                    MemoryToggle("Remember game sessions and participants",
                        "Resume Tambola, RPS/Tic-tac-toe tallies, team participants and scoreboards. Turning this off stops future automatic loading and saving, without interrupting the current screen.",
                        settings.rememberGameSessions, !busy) { save(settings.copy(rememberGameSessions = it)) }
                    Text("Existing saved games are kept when this is off. Re-enabling can resume the old saved game, even after resetting a temporary session. Clear saved game sessions below to remove it. Older game storage may be included in Android backup.",
                        style = MaterialTheme.typography.bodySmall)
                    TextButton(enabled = !busy, onClick = { clearTarget = "game sessions" }) { Text("Clear saved game sessions") }
                    HorizontalDivider()
                    MemoryToggle("Keep tool history", "Save completed inputs and results in supported tools. Turning this off pauses collection; it does not delete existing history.",
                        settings.historyEnabled, !busy) { save(settings.copy(historyEnabled = it)) }
                    HorizontalDivider()
                    Text("Text, QR and Hash privacy", style = MaterialTheme.typography.titleSmall)
                    Text("These tools can contain secrets or personal information. Automatic saving is off for each by default. Each choice below also requires its global history or last-result switch above to be on.",
                        style = MaterialTheme.typography.bodySmall)
                    Text("Turning these choices off retains existing records without loading them automatically. Review or explicitly clear them below. Android task drafts can still survive screen recreation; these switches do not disable task drafts. Manually saved Workbench items and presets are separate and remain until you delete them there.",
                        style = MaterialTheme.typography.bodySmall)
                    privateInputTools.forEach { (route, name) ->
                        val policy = settings.automaticMemoryFor(route)
                        val counts = savedCounts[route] ?: (0 to 0)
                        Text(name, style = MaterialTheme.typography.titleSmall)
                        MemoryToggle("$name: automatic history", "Allow completed inputs and outputs to be saved when Keep tool history is on.",
                            policy.historyEnabled, !busy) { value ->
                            save(settings.copy(automaticMemory = settings.automaticMemory +
                                (route to policy.copy(historyEnabled = value))))
                        }
                        MemoryToggle("$name: remember last result", "Allow inputs and output to be retained for later use when Remember last result is on. QR and Hash require Generate.",
                            policy.rememberResults, !busy) { value ->
                            save(settings.copy(automaticMemory = settings.automaticMemory +
                                (route to policy.copy(rememberResults = value))))
                        }
                        Text("${counts.first} history entries · ${counts.second} remembered result", style = MaterialTheme.typography.bodySmall)
                        TextButton(enabled = !busy, onClick = {
                            busy = true
                            scope.launch {
                                try {
                                    reviewEntries = withContext(Dispatchers.IO) {
                                        buildList {
                                            store.rememberedResult(route)?.let { add("Remembered result" to it) }
                                            store.history(route).forEachIndexed { index, entry -> add("History ${index + 1}" to entry) }
                                        }
                                    }
                                    reviewTitle = name
                                } finally { busy = false }
                            }
                        }) { Text("Review $name saved data") }
                        TextButton(enabled = !busy && counts.first > 0,
                            onClick = { clearTarget = "$route|history" }) { Text("Clear $name history") }
                        TextButton(enabled = !busy && counts.second > 0,
                            onClick = { clearTarget = "$route|result" }) { Text("Clear $name remembered result") }
                    }
                    HorizontalDivider()
                    OutlinedTextField(
                        value = limitText,
                        onValueChange = { if (it.length <= 3 && it.all(Char::isDigit)) limitText = it },
                        label = { Text("Entries per tool") },
                        supportingText = { Text("Choose 1–100. Lowering the limit removes the oldest entries.") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val limit = limitText.toIntOrNull()
                    TextButton(enabled = !busy && limit != null && limit in 1..100 && limit != settings.historyLimit,
                        onClick = { limit?.let { save(settings.copy(historyLimit = it)) } }) {
                        Text("Apply history limit")
                    }
                    TextButton(enabled = !busy, onClick = { clearTarget = "history" }) { Text("Clear all tool history") }
                    TextButton(enabled = !busy, onClick = { clearTarget = "modes" }) { Text("Clear remembered modes") }
                    TextButton(enabled = !busy, onClick = { clearTarget = "results" }) { Text("Clear remembered results") }
                    Text("Turning off mode or result memory stops using it without deleting it. Use Clear to remove it.",
                        style = MaterialTheme.typography.bodySmall)
                    feedback?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    Text("These choices do not reset active timers, scores or saved favorites.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { TextButton(enabled = !busy, onClick = { open = false }) { Text("Done") } },
        )
    }
    reviewTitle?.let { name ->
        AlertDialog(
            onDismissRequest = { reviewTitle = null; reviewEntries = emptyList() },
            title = { Text("$name saved data") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (reviewEntries.isEmpty()) Text("No automatic history or remembered result is stored for this tool.")
                    reviewEntries.forEach { (label, entry) ->
                        Text(label, style = MaterialTheme.typography.titleSmall)
                        Text(entry.inputs.entries.joinToString("\n") { (key, value) -> "$key: $value" })
                        Text(entry.output)
                        HorizontalDivider()
                    }
                }
            },
            confirmButton = { TextButton(onClick = { reviewTitle = null; reviewEntries = emptyList() }) { Text("Close review") } },
        )
    }
    clearTarget?.let { target ->
        val route = target.substringBefore('|').takeIf { candidate -> privateInputTools.any { it.first == candidate } }
        val name = privateInputTools.firstOrNull { it.first == route }?.second
        val kind = target.substringAfter('|')
        val targetLabel = if (route != null) "$name ${if (kind == "history") "history" else "remembered result"}" else "remembered $target"
        AlertDialog(
            onDismissRequest = { clearTarget = null },
            title = { Text("Clear $targetLabel?") },
            text = { Text(if (route != null)
                "Remove only $targetLabel from this device? This cannot be undone. Manual Workbench items, presets, modes and current Android task drafts are unchanged. An open tool may save again if automatic saving remains enabled."
                else if (target == "game sessions")
                "Remove saved Tambola numbers, RPS tallies, team participants and both scoreboards, including dormant data while remembering is off? This cannot be undone. Current open sessions, counters, timers, favorites, palettes and tool history are unchanged. An open session can save again if remembering remains on."
                else "This removes saved $target from every supported tool on this device. It cannot be undone. Favorites and active tool sessions are unchanged.") },
            confirmButton = { TextButton(enabled = !busy, onClick = {
                busy = true
                clearTarget = null
                scope.launch {
                    try {
                        val success = withContext(Dispatchers.IO) {
                            if (route != null) {
                                if (kind == "history") store.clearHistory(route) else store.clearRememberedResults(route)
                            } else when (target) {
                                "game sessions" -> sessions.clearSavedSessions()
                                "modes" -> store.clearRememberedModes()
                                "results" -> store.clearRememberedResults()
                                else -> store.clearHistory()
                            }
                        }
                        refreshCounts()
                        feedback = if (success) "$targetLabel cleared." else "Could not clear $targetLabel. Please try again."
                    } finally { busy = false }
                }
            }) { Text("Clear") } },
            dismissButton = { TextButton(enabled = !busy, onClick = { clearTarget = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun MemoryToggle(title: String, description: String, value: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
        trailingContent = { Switch(checked = value, enabled = enabled, onCheckedChange = onChange,
            modifier = Modifier.semantics { contentDescription = title }) },
    )
}
