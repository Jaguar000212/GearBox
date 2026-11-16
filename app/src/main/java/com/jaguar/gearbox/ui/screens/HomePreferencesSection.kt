package com.jaguar.gearbox.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.RecentToolsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun HomePreferencesSection(store: RecentToolsStore) {
    var open by rememberSaveable { mutableStateOf(false) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    fun update(action: () -> Boolean, successMessage: String? = null) {
        if (busy) return
        busy = true
        scope.launch {
            val success = withContext(Dispatchers.IO) { action() }
            feedback = if (success) successMessage else "Could not save this change. Please try again."
            busy = false
        }
    }
    Text("Home & Favorites", style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp))
    ListItem(
        headlineContent = { Text("Recent tools") },
        supportingContent = { Text("${if (store.showRecentTools) "Visible" else "Hidden"} · ${if (store.recordRecentTools) "Recording" else "Collection paused"}") },
        modifier = Modifier.clickable { open = true; feedback = null },
    )
    if (open) AlertDialog(
        onDismissRequest = { if (!busy) open = false },
        title = { Text("Recent tools") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("A shortcut to your last ${RecentToolsStore.MAX_RECENTS} tools. This stores only tool names/routes, never their inputs or results. Kept on this device, outside cloud backup.")
                ListItem(
                    headlineContent = { Text("Show recent tools") },
                    supportingContent = { Text("Display the recent-tools row on Home. Hiding it does not stop collection.") },
                    trailingContent = { Switch(checked = store.showRecentTools, enabled = !busy,
                        modifier = Modifier.semantics { contentDescription = "Show recent tools" },
                        onCheckedChange = { value -> update({ store.setShowRecentTools(value) }) }) },
                )
                ListItem(
                    headlineContent = { Text("Record recent tools") },
                    supportingContent = { Text("Update the list when a tool opens. Pausing keeps the existing list unchanged.") },
                    trailingContent = { Switch(checked = store.recordRecentTools, enabled = !busy,
                        modifier = Modifier.semantics { contentDescription = "Record recent tools" },
                        onCheckedChange = { value -> update({ store.setRecordRecentTools(value) }) }) },
                )
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                TextButton(enabled = !busy && store.recent.isNotEmpty(), onClick = { confirmClear = true }) {
                    Text("Clear recent history")
                }
                feedback?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                Text("Tool history, saved results, favorites and active timers are separate and will not be changed.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(enabled = !busy, onClick = { open = false }) { Text("Done") } },
    )
    if (confirmClear) AlertDialog(
        onDismissRequest = { if (!busy) confirmClear = false },
        title = { Text("Clear recent history?") },
        text = { Text("Remove the list of recently opened tools from this device? This cannot be undone. New tools will be recorded again if Record recent tools stays on.") },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                confirmClear = false
                update({ store.clear() }, "Recent history cleared.")
            }) { Text("Clear") }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = { confirmClear = false }) { Text("Cancel") } },
    )
}
