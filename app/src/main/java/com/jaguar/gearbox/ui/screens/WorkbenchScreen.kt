package com.jaguar.gearbox.ui.screens

import com.jaguar.gearbox.data.localizedInputLabel
import com.jaguar.gearbox.data.localizedName
import com.jaguar.gearbox.R
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jaguar.gearbox.data.savedResultComparison
import com.jaguar.gearbox.ui.components.AdaptiveToolLayout
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
    initialPresetsSelected: Boolean = false,
) {
    val context = LocalContext.current
    val store = remember(context, memoryStore) { memoryStore ?: ToolMemoryStore(context) }
    val scope = rememberCoroutineScope()
    var presetsSelected by rememberSaveable { mutableStateOf(initialPresetsSelected) }
    var results by remember { mutableStateOf<List<SavedToolItem>>(emptyList()) }
    var presets by remember { mutableStateOf<List<SavedToolItem>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var sharePreview by remember { mutableStateOf<String?>(null) }
    var handoffTarget by remember { mutableStateOf<SavedToolItem?>(null) }
    var deleteTarget by remember { mutableStateOf<SavedToolItem?>(null) }
    var deletePreset by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var selecting by rememberSaveable { mutableStateOf(false) }
    var selection by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var renameTarget by remember { mutableStateOf<SavedToolItem?>(null) }
    var renamePreset by remember { mutableStateOf(false) }
    var renameText by rememberSaveable { mutableStateOf("") }
    var batchTargets by remember { mutableStateOf<List<SavedToolItem>>(emptyList()) }
    var batchPreset by remember { mutableStateOf(false) }
    var comparison by remember { mutableStateOf<List<SavedToolItem>>(emptyList()) }
    val items = if (presetsSelected) presets else results
    val visibleItems = items.filter { item -> query.isBlank() || item.name.contains(query.trim(), true) ||
        Tools.byRoute(item.route)?.localizedName(context)?.contains(query.trim(), true) == true }
    val selectedItems = visibleItems.filter { it.id in selection }
    fun leaveSelection() { selecting = false; selection = emptyList() }
    BackHandler(enabled = selecting && renameTarget == null && batchTargets.isEmpty() && comparison.isEmpty()) {
        if (!busy) leaveSelection()
    }
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
                    IconButton(enabled = !busy, onClick = { if (selecting) leaveSelection() else onNavigateBack() }) {
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
                        FilterChip(selected = !presetsSelected, enabled = !busy, onClick = { if (presetsSelected) { presetsSelected = false; leaveSelection() } },
                            label = { Text(context.getString(R.string.screen_results_1_s, results.size)) })
                        FilterChip(selected = presetsSelected, enabled = !busy, onClick = { if (!presetsSelected) { presetsSelected = true; leaveSelection() } },
                            label = { Text(context.getString(R.string.screen_presets_1_s, presets.size)) })
                    }
                    Text(context.resources.getQuantityString(if (presetsSelected) R.plurals.screen_presets_capacity else R.plurals.screen_results_capacity, items.size, items.size, ToolMemoryStore.MAX_SAVED_ITEMS),
                        style = MaterialTheme.typography.labelMedium)
                    Text(context.getString(R.string.workbench_private_notice), style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(query, { query = it; leaveSelection() }, enabled = !busy,
                        label = { Text(context.getString(R.string.workbench_search)) }, singleLine = true,
                        modifier = Modifier.fillMaxWidth())
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(enabled = loaded && !busy && (items.isNotEmpty() || selecting),
                            onClick = { if (selecting) leaveSelection() else selecting = true }) {
                            Text(context.getString(if (selecting) R.string.workbench_cancel_selection else R.string.workbench_select_items))
                        }
                        if (selecting) {
                            Text(context.resources.getQuantityString(R.plurals.workbench_selected_count, selectedItems.size, selectedItems.size))
                            TextButton(enabled = !busy && visibleItems.isNotEmpty(), onClick = { selection = visibleItems.map { it.id } }) {
                                Text(context.getString(R.string.workbench_select_visible))
                            }
                            TextButton(enabled = !busy && selectedItems.isNotEmpty(), onClick = {
                                batchPreset = presetsSelected; batchTargets = selectedItems.toList(); error = null
                            }) { Text(context.getString(R.string.workbench_delete_selected)) }
                            if (!presetsSelected) {
                                val canCompare = selectedItems.size == 2 && savedResultComparison(selectedItems[0], selectedItems[1]).compatible
                                TextButton(enabled = !busy && canCompare, onClick = { comparison = selectedItems.toList() }) {
                                    Text(context.getString(R.string.workbench_compare))
                                }
                            }
                        }
                    }
                    if (selecting && !presetsSelected) Text(context.getString(R.string.workbench_compare_guidance), style = MaterialTheme.typography.bodySmall)
                    if (loaded && items.isNotEmpty() && visibleItems.isEmpty()) Text(context.getString(R.string.workbench_no_matches))
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
            items(visibleItems, key = { it.id }) { item ->
                val tool = Tools.all.firstOrNull { it.route == item.route }
                OutlinedCard(Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (selecting) Row(Modifier.fillMaxWidth().toggleable(
                            value = item.id in selection, enabled = !busy, role = Role.Checkbox,
                            onValueChange = { checked -> selection = if (checked) selection + item.id else selection - item.id }),
                            verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = item.id in selection, onCheckedChange = null)
                            Text(item.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        } else Text(item.name, style = MaterialTheme.typography.titleLarge)
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
                        if (!selecting) FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                renamePreset = presetsSelected; renameTarget = item; renameText = item.name; error = null
                            }) { Text(context.getString(R.string.workbench_rename)) }
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
    renameTarget?.let { target ->
        AlertDialog(onDismissRequest = { if (!busy) renameTarget = null },
            title = { Text(context.getString(R.string.workbench_rename)) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(renameText, { renameText = it }, enabled = !busy,
                    label = { Text(context.getString(R.string.workbench_name)) }, singleLine = true,
                    isError = renameText.trim().length > 80,
                    supportingText = { Text(context.getString(R.string.workbench_name_count, renameText.trim().length)) })
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            } },
            confirmButton = { TextButton(enabled = !busy && renameText.trim().length in 1..80, onClick = {
                busy = true
                val name = renameText.trim(); val isPreset = renamePreset
                scope.launch {
                    try {
                        val success = withContext(Dispatchers.IO) { if (isPreset) store.renamePreset(target.id, name) else store.renameSavedResult(target.id, name) }
                        if (success) {
                            if (isPreset) presets = withContext(Dispatchers.IO) { store.presets() }
                            else results = withContext(Dispatchers.IO) { store.savedResults() }
                            renameTarget = null; error = null
                        } else error = context.getString(R.string.workbench_rename_failed)
                    } finally { busy = false }
                }
            }) { Text(context.getString(R.string.workbench_save_name)) } },
            dismissButton = { TextButton(enabled = !busy, onClick = { renameTarget = null }) { Text(context.getString(R.string.screen_cancel)) } })
    }
    if (batchTargets.isNotEmpty()) AlertDialog(onDismissRequest = { if (!busy) batchTargets = emptyList() },
        title = { Text(context.resources.getQuantityString(R.plurals.workbench_delete_count, batchTargets.size, batchTargets.size)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(context.getString(if (batchPreset) R.string.workbench_delete_presets_notice else R.string.workbench_delete_results_notice))
            batchTargets.forEach { Text(it.name) }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } },
        confirmButton = { TextButton(enabled = !busy, onClick = {
            busy = true
            val ids = batchTargets.map { it.id }.toSet(); val isPreset = batchPreset
            scope.launch {
                try {
                    val success = withContext(Dispatchers.IO) { if (isPreset) store.removePresets(ids) else store.removeSavedResults(ids) }
                    if (success) {
                        if (isPreset) presets = withContext(Dispatchers.IO) { store.presets() }
                        else results = withContext(Dispatchers.IO) { store.savedResults() }
                        batchTargets = emptyList(); leaveSelection(); error = null
                    } else error = context.getString(R.string.screen_could_not_delete_the_saved_item_please_try_again)
                } finally { busy = false }
            }
        }) { Text(context.getString(R.string.screen_delete)) } },
        dismissButton = { TextButton(enabled = !busy, onClick = { batchTargets = emptyList() }) { Text(context.getString(R.string.screen_cancel)) } })
    if (comparison.size == 2) SavedResultComparisonDialog(comparison[0], comparison[1]) { comparison = emptyList() }
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


/** Read-only snapshots: this dialog never restores inputs, recalculates, copies or shares. */
@Composable
internal fun SavedResultComparisonDialog(first: SavedToolItem, second: SavedToolItem, onDismiss: () -> Unit) {
    val context = LocalContext.current
    if (!savedResultComparison(first, second).compatible) return
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(context.getString(R.string.workbench_comparison_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text(context.getString(R.string.workbench_close_comparison)) }
                }
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(context.getString(R.string.workbench_snapshot_notice))
                    AdaptiveToolLayout(input = { ComparisonSnapshot(first) }, result = { ComparisonSnapshot(second) })
                }
            }
        }
    }
}

@Composable
private fun ComparisonSnapshot(item: SavedToolItem) {
    val context = LocalContext.current
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(item.name, style = MaterialTheme.typography.titleLarge)
            Text(Tools.byRoute(item.route)?.localizedName(context).orEmpty(), style = MaterialTheme.typography.labelLarge)
            Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(item.timestampMillis)))
            Text(context.getString(R.string.workbench_saved_inputs), style = MaterialTheme.typography.titleMedium)
            Text(item.inputs.filterKeys { !it.startsWith("_") }.entries.joinToString("\n") { "${localizedInputLabel(context, it.key)}: ${it.value}" })
            HorizontalDivider()
            Text(context.getString(R.string.workbench_saved_output), style = MaterialTheme.typography.titleMedium)
            Text(item.output)
        }
    }
}
