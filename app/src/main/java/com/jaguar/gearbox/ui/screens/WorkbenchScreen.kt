package com.jaguar.gearbox.ui.screens

import com.jaguar.gearbox.data.localizedInputLabel
import com.jaguar.gearbox.data.localizedName
import com.jaguar.gearbox.R
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
import com.jaguar.gearbox.ui.components.SensitiveTextShareDialog
import com.jaguar.gearbox.data.ToolMemoryPolicy
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
    var sharePreview by remember { mutableStateOf<String?>(null) }
    var handoffTarget by remember { mutableStateOf<SavedToolItem?>(null) }
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
                title = { Text(context.getString(R.string.screen_workbench)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = context.getString(R.string.screen_back))
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
                    Text(context.getString(R.string.screen_keep_what_matters), style = MaterialTheme.typography.headlineSmall)
                    Text(context.getString(R.string.screen_named_results_and_reusable_setups_saved_only_on_this_device_nothi),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !presetsSelected, onClick = { presetsSelected = false },
                            label = { Text(context.getString(R.string.screen_results_1_s, results.size)) })
                        FilterChip(selected = presetsSelected, onClick = { presetsSelected = true },
                            label = { Text(context.getString(R.string.screen_presets_1_s, presets.size)) })
                    }
                    Text(context.resources.getQuantityString(if (presetsSelected) R.plurals.screen_presets_capacity else R.plurals.screen_results_capacity, items.size, items.size, ToolMemoryStore.MAX_SAVED_ITEMS),
                        style = MaterialTheme.typography.labelMedium)
                    if (!loaded || busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
            if (loaded && items.isEmpty()) item {
                OutlinedCard(Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (presetsSelected) context.getString(R.string.screen_your_shortcuts_to_a_ready_to_use_tool) else context.getString(R.string.screen_a_place_for_results_worth_keeping),
                            style = MaterialTheme.typography.titleMedium)
                        Text(if (presetsSelected)
                            context.getString(R.string.screen_open_history_save_setup_in_a_supported_tool_to_name_a_setup_you_w)
                            else context.getString(R.string.screen_open_history_keep_result_in_a_supported_tool_to_keep_its_inputs_a))
                    }
                }
            }
            items(items, key = { it.id }) { item ->
                val tool = Tools.all.firstOrNull { it.route == item.route }
                OutlinedCard(Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(item.name, style = MaterialTheme.typography.titleLarge)
                        Text(tool?.localizedName(context) ?: context.getString(R.string.screen_unavailable_tool), style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary)
                        Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                            .format(Date(item.timestampMillis)), style = MaterialTheme.typography.labelSmall)
                        if (item.inputs.isNotEmpty()) Text(item.inputs.filterKeys { !it.startsWith("_") }.entries.joinToString("\n") { "${localizedInputLabel(context, it.key)}: ${it.value}" },
                            style = MaterialTheme.typography.bodyMedium)
                        if (item.output.isNotBlank()) {
                            HorizontalDivider()
                            Text(item.output, style = MaterialTheme.typography.bodyLarge)
                            Text(context.getString(R.string.screen_saved_snapshot_restore_supported_inputs_or_validated_outcomes_phy),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { context.copyToClipboard(item.name, savedItemText(context, item), sensitive = ToolMemoryPolicy.isSensitiveTextRoute(item.route)) }) { Text(context.getString(R.string.screen_copy)) }
                            TextButton(onClick = {
                                if (ToolMemoryPolicy.isSensitiveTextRoute(item.route)) sharePreview = savedItemText(context, item)
                                else runCatching { context.shareText(savedItemText(context, item)) }
                                    .onFailure { error = context.getString(R.string.screen_no_sharing_app_is_available_you_can_copy_this_item_instead) }
                            }) { Text(context.getString(R.string.screen_share)) }
                            TextButton(enabled = tool != null && !busy && (presetsSelected || com.jaguar.gearbox.data.canRestoreToolSnapshot(item.route)),
                                onClick = { onOpen(item.route, item.inputs, presetsSelected) }) {
                                Text(if (presetsSelected) context.getString(R.string.screen_use_preset) else context.getString(R.string.screen_restore))
                            }
                            if (!presetsSelected && com.jaguar.gearbox.data.compatibleToolHandoffs(item.route, item.inputs, item.output).isNotEmpty()) {
                                TextButton(enabled = !busy, onClick = { handoffTarget = item }) { Text(context.getString(R.string.screen_use_in)) }
                            }
                            TextButton(enabled = !busy, onClick = {
                                deletePreset = presetsSelected
                                deleteTarget = item
                            }) { Text(context.getString(R.string.screen_delete)) }
                        }
                    }
                }
            }
        }
    }
    sharePreview?.let { text ->
        SensitiveTextShareDialog(text, onConfirm = {
            sharePreview = null
            runCatching { context.shareText(text) }
                .onFailure { error = context.getString(R.string.screen_no_sharing_app_is_available_you_can_copy_this_item_instead) }
        }, onDismiss = { sharePreview = null })
    }
    handoffTarget?.let { item ->
        CompositionLocalProvider(com.jaguar.gearbox.ui.components.LocalToolHandoff provides { route: String, inputs: Map<String, String> ->
            onOpen(route, inputs, false)
        }) {
            com.jaguar.gearbox.ui.components.ToolHandoffDialog(
                com.jaguar.gearbox.ui.components.ToolHistoryBinding(item.route, {}, item.inputs, item.output),
            ) { handoffTarget = null }
        }
    }
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { if (!busy) deleteTarget = null },
            title = { Text(context.getString(if (deletePreset) R.string.screen_delete_preset_question else R.string.screen_delete_result_question)) },
            text = { Text(context.getString(R.string.screen_remove_1_s_from_this_device_this_cannot_be_undone_tool_history_an, target.name)) },
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
                            error = context.getString(R.string.screen_could_not_delete_the_saved_item_please_try_again)
                            deleteTarget = null
                        }
                        busy = false
                    }
                }) { Text(context.getString(R.string.screen_delete)) }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { deleteTarget = null }) { Text(context.getString(R.string.screen_cancel)) } },
        )
    }
}

private fun savedItemText(context: android.content.Context, item: SavedToolItem): String = buildString {
    append(item.name)
    if (item.inputs.isNotEmpty()) append("\n").append(item.inputs.filterKeys { !it.startsWith("_") }.entries.joinToString("\n") { "${localizedInputLabel(context, it.key)}: ${it.value}" })
    if (item.output.isNotBlank()) append("\n\n").append(item.output)
}
