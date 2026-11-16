package com.jaguar.gearbox.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ToolPresetsDialog(binding: ToolHistoryBinding, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { ToolMemoryStore(context) }
    val scope = rememberCoroutineScope()
    val apply = LocalToolPresetOpener.current
    val workbench = LocalWorkbenchOpener.current
    var presets by remember { mutableStateOf<List<SavedToolItem>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(binding.route) {
        presets = withContext(Dispatchers.IO) { store.presets().filter { it.route == binding.route } }
        loaded = true
    }
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text("Presets") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Reuse a named setup without changing unrelated inputs. Presets are explicitly saved on this device.")
                if (!loaded || busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (loaded && presets.isEmpty()) Text("No presets for this tool yet.")
                LazyColumn(Modifier.heightIn(max = 240.dp)) {
                    items(presets, key = { it.id }) { preset ->
                        ListItem(headlineContent = { Text(preset.name) },
                            supportingContent = { Text(preset.inputs.entries.joinToString(" · ") { "${it.key}: ${it.value}" }) },
                            modifier = Modifier.clickable(enabled = apply != null && !busy) {
                                onDismiss(); apply?.invoke(preset.inputs)
                            })
                    }
                }
                if (binding.configuration.isNotEmpty()) {
                    OutlinedTextField(value = name, onValueChange = { name = it.take(80) },
                        label = { Text("Name this setup") }, enabled = !busy, singleLine = true)
                    TextButton(enabled = name.isNotBlank() && !busy, onClick = {
                        if (!busy) {
                            busy = true
                            val label = name.trim()
                            val configuration = binding.configuration.toMap()
                            scope.launch {
                                val saved = withContext(Dispatchers.IO) { store.savePreset(label, binding.route, configuration) }
                                if (saved) {
                                    presets = withContext(Dispatchers.IO) { store.presets().filter { it.route == binding.route } }
                                    name = ""; feedback = "Preset saved."
                                } else feedback = "Could not save. Check the 100-preset limit and input size."
                                busy = false
                            }
                        }
                    }) { Text("Save current setup") }
                }
                feedback?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Close") } },
        dismissButton = { if (workbench != null) TextButton(enabled = !busy, onClick = { onDismiss(); workbench() }) { Text("Open Workbench") } },
    )
}

@Composable
fun ToolHandoffDialog(binding: ToolHistoryBinding, onDismiss: () -> Unit) {
    val handoff = LocalToolHandoff.current
    val choices = compatibleToolHandoffs(binding.route, binding.inputs, binding.output)
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Use result in…") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Choose a destination. This passes the current result or its explicitly labeled number only; it never reads your clipboard or starts a timer or game.")
                Text(binding.output.orEmpty().take(200), style = MaterialTheme.typography.bodySmall, maxLines = 4,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(choices, key = { it.route }) { choice ->
                        ListItem(headlineContent = { Text(choice.label) }, supportingContent = { Text(choice.detail) },
                            modifier = Modifier.clickable(enabled = handoff != null) {
                                onDismiss(); handoff?.invoke(choice.route, choice.inputs)
                            })
                    }
                    if (choices.isEmpty()) item { Text("No compatible result is available yet.") }
                }
            }
        }, confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

val LocalWorkbenchOpener = staticCompositionLocalOf<(() -> Unit)?> { null }
