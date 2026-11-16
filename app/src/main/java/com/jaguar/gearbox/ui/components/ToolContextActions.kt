package com.jaguar.gearbox.ui.components

import com.jaguar.gearbox.data.localized
import com.jaguar.gearbox.data.localizedInputLabel
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
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text(context.getString(com.jaguar.gearbox.R.string.shared_presets)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(context.getString(com.jaguar.gearbox.R.string.shared_reuse_a_named_setup_without_changing_unrelated_inputs_presets_are_expl))
                if (!loaded || busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (loaded && presets.isEmpty()) Text(context.getString(com.jaguar.gearbox.R.string.shared_no_presets_for_this_tool_yet))
                LazyColumn(Modifier.heightIn(max = 240.dp)) {
                    items(presets, key = { it.id }) { preset ->
                        ListItem(headlineContent = { Text(preset.name) },
                            supportingContent = { Text(preset.inputs.entries.joinToString(" · ") { context.getString(com.jaguar.gearbox.R.string.shared_label_value, localizedInputLabel(context, it.key), it.value) }) },
                            modifier = Modifier.clickable(enabled = apply != null && !busy) {
                                onDismiss(); apply?.invoke(preset.inputs)
                            })
                    }
                }
                if (binding.configuration.isNotEmpty()) {
                    OutlinedTextField(value = name, onValueChange = { name = it.take(80) },
                        label = { Text(context.getString(com.jaguar.gearbox.R.string.shared_name_this_setup)) }, enabled = !busy, singleLine = true)
                    TextButton(enabled = name.isNotBlank() && !busy, onClick = {
                        if (!busy) {
                            busy = true
                            val label = name.trim()
                            val configuration = binding.configuration.toMap()
                            scope.launch {
                                val saved = withContext(Dispatchers.IO) { store.savePreset(label, binding.route, configuration) }
                                if (saved) {
                                    presets = withContext(Dispatchers.IO) { store.presets().filter { it.route == binding.route } }
                                    name = ""; feedback = context.getString(com.jaguar.gearbox.R.string.shared_preset_saved)
                                } else feedback = context.getString(com.jaguar.gearbox.R.string.shared_could_not_save_check_the_100_preset_limit_and_input_size)
                                busy = false
                            }
                        }
                    }) { Text(context.getString(com.jaguar.gearbox.R.string.shared_save_current_setup)) }
                }
                feedback?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text(context.getString(com.jaguar.gearbox.R.string.shared_close)) } },
        dismissButton = { if (workbench != null) TextButton(enabled = !busy, onClick = { onDismiss(); workbench() }) { Text(context.getString(com.jaguar.gearbox.R.string.shared_open_workbench)) } },
    )
}

@Composable
fun ToolHandoffDialog(binding: ToolHistoryBinding, onDismiss: () -> Unit) {
    val handoff = LocalToolHandoff.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val choices = compatibleToolHandoffs(binding.route, binding.inputs, binding.output).map { it.localized(context) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(context.getString(com.jaguar.gearbox.R.string.shared_use_result_in)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(context.getString(com.jaguar.gearbox.R.string.shared_choose_a_destination_this_passes_the_current_result_or_its_explicitly))
                Text(binding.output.orEmpty().take(200), style = MaterialTheme.typography.bodySmall, maxLines = 4,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(choices, key = { it.route }) { choice ->
                        ListItem(headlineContent = { Text(choice.label) }, supportingContent = { Text(choice.detail) },
                            modifier = Modifier.clickable(enabled = handoff != null) {
                                onDismiss(); handoff?.invoke(choice.route, choice.inputs)
                            })
                    }
                    if (choices.isEmpty()) item { Text(context.getString(com.jaguar.gearbox.R.string.shared_no_compatible_result_is_available_yet)) }
                }
            }
        }, confirmButton = { TextButton(onClick = onDismiss) { Text(context.getString(com.jaguar.gearbox.R.string.shared_cancel)) } },
    )
}

val LocalWorkbenchOpener = staticCompositionLocalOf<(() -> Unit)?> { null }
