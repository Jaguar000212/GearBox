package com.jaguar.gearbox.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.SavedToolItem
import com.jaguar.gearbox.data.ToolMemoryStore
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.tools.copyToClipboard
import com.jaguar.gearbox.ui.tools.shareText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

/** Explicit saves are distinct from rolling history and never expire to make room for new saves. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WorkbenchScreen(
    onNavigateBack: () -> Unit,
    onOpen: (String, Map<String, String>, Boolean) -> Unit,
    memoryStore: ToolMemoryStore? = null,
) {
    val context = LocalContext.current
    val store = remember(context, memoryStore) { memoryStore ?: ToolMemoryStore(context) }
    val scope = rememberCoroutineScope()
    var presetsSelected by rememberSaveable { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<SavedToolItem>>(emptyList()) }
    var presets by remember { mutableStateOf<List<SavedToolItem>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<SavedToolItem?>(null) }
    var deletePreset by remember { mutableStateOf(false) }
    val items = if (presetsSelected) presets else results
    LaunchedEffect(store) {
        withContext(Dispatchers.IO) { store.savedResults() to store.presets() }.let {
            results = it.first
            presets = it.second
            loaded = true
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workbench") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Column(Modifier.widthIn(max = 840.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Keep what matters", style = MaterialTheme.typography.headlineSmall)
                    Text("Named results and reusable setups, saved only on this device. Nothing is added automatically.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !presetsSelected, onClick = { presetsSelected = false },
                            label = { Text("Results (${results.size})") })
                        FilterChip(selected = presetsSelected, onClick = { presetsSelected = true },
                            label = { Text("Presets (${presets.size})") })
                    }
                    Text("${items.size} of ${ToolMemoryStore.MAX_SAVED_ITEMS} ${if (presetsSelected) "presets" else "results"}",
                        style = MaterialTheme.typography.labelMedium)
                    if (!loaded || busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
            if (loaded && items.isEmpty()) item {
                OutlinedCard(Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (presetsSelected) "Your shortcuts to a ready-to-use tool" else "A place for results worth keeping",
                            style = MaterialTheme.typography.titleMedium)
                        Text(if (presetsSelected)
                            "Open History → Save setup in a supported tool to name a setup you want to reuse."
                            else "Open History → Keep result in a supported tool to keep its inputs and output together.")
                    }
                }
            }
            items(items, key = { it.id }) { item ->
                val tool = Tools.all.firstOrNull { it.route == item.route }
                OutlinedCard(Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(item.name, style = MaterialTheme.typography.titleLarge)
                        Text(tool?.name ?: "Unavailable tool", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary)
                        Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                            .format(Date(item.timestampMillis)), style = MaterialTheme.typography.labelSmall)
                        if (item.inputs.isNotEmpty()) Text(item.inputs.filterKeys { !it.startsWith("_") }.entries.joinToString("\n") { "${it.key}: ${it.value}" },
                            style = MaterialTheme.typography.bodyMedium)
                        if (item.output.isNotBlank()) {
                            HorizontalDivider()
                            Text(item.output, style = MaterialTheme.typography.bodyLarge)
                            Text("Saved snapshot. Restore inputs to recalculate with the current tool.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { context.copyToClipboard(item.name, savedItemText(item)) }) { Text("Copy") }
                            TextButton(onClick = {
                                runCatching { context.shareText(savedItemText(item)) }
                                    .onFailure { error = "No sharing app is available. You can copy this item instead." }
                            }) { Text("Share") }
                            TextButton(enabled = tool != null && !busy,
                                onClick = { onOpen(item.route, item.inputs, presetsSelected) }) {
                                Text(if (presetsSelected) "Use preset" else "Restore inputs")
                            }
                            TextButton(enabled = !busy, onClick = {
                                deletePreset = presetsSelected
                                deleteTarget = item
                            }) { Text("Delete") }
                        }
                    }
                }
            }
        }
    }
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { if (!busy) deleteTarget = null },
            title = { Text("Delete ${if (deletePreset) "preset" else "saved result"}?") },
            text = { Text("Remove “${target.name}” from this device? This cannot be undone. Tool history and current inputs are unchanged.") },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    busy = true
                    val isPreset = deletePreset
                    scope.launch {
                        val success = withContext(Dispatchers.IO) {
                            if (isPreset) store.removePreset(target.id) else store.removeSavedResult(target.id)
                        }
                        if (success) {
                            if (isPreset) presets = presets.filterNot { it.id == target.id }
                            else results = results.filterNot { it.id == target.id }
                            error = null
                            deleteTarget = null
                        } else {
                            error = "Could not delete the saved item. Please try again."
                            deleteTarget = null
                        }
                        busy = false
                    }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { deleteTarget = null }) { Text("Cancel") } },
        )
    }
}

private fun savedItemText(item: SavedToolItem): String = buildString {
    append(item.name)
    if (item.inputs.isNotEmpty()) append("\n").append(item.inputs.filterKeys { !it.startsWith("_") }.entries.joinToString("\n") { "${it.key}: ${it.value}" })
    if (item.output.isNotBlank()) append("\n\n").append(item.output)
}
