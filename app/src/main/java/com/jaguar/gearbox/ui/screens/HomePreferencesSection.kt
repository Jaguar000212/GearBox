package com.jaguar.gearbox.ui.screens

import com.jaguar.gearbox.R
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
    val context = androidx.compose.ui.platform.LocalContext.current
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
            feedback = if (success) successMessage else context.getString(R.string.screen_could_not_save_this_change_please_try_again)
            busy = false
        }
    }
    Text(context.getString(R.string.screen_home_favorites), style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp))
    ListItem(
        headlineContent = { Text(context.getString(R.string.screen_recent_tools)) },
        supportingContent = { Text(context.getString(R.string.screen_recent_status, context.getString(if (store.showRecentTools) R.string.screen_visible else R.string.screen_hidden), context.getString(if (store.recordRecentTools) R.string.screen_recording else R.string.screen_collection_paused))) },
        modifier = Modifier.clickable { open = true; feedback = null },
    )
    if (open) AlertDialog(
        onDismissRequest = { if (!busy) open = false },
        title = { Text(context.getString(R.string.screen_recent_tools)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(context.getString(R.string.screen_a_shortcut_to_your_last_1_s_tools_this_stores_only_tool_names_rou, RecentToolsStore.MAX_RECENTS))
                ListItem(
                    headlineContent = { Text(context.getString(R.string.screen_show_recent_tools)) },
                    supportingContent = { Text(context.getString(R.string.screen_display_the_recent_tools_row_on_home_hiding_it_does_not_stop_coll)) },
                    trailingContent = { Switch(checked = store.showRecentTools, enabled = !busy,
                        modifier = Modifier.semantics { contentDescription = context.getString(R.string.screen_show_recent_tools) },
                        onCheckedChange = { value -> update({ store.setShowRecentTools(value) }) }) },
                )
                ListItem(
                    headlineContent = { Text(context.getString(R.string.screen_record_recent_tools)) },
                    supportingContent = { Text(context.getString(R.string.screen_update_the_list_when_a_tool_opens_pausing_keeps_the_existing_list)) },
                    trailingContent = { Switch(checked = store.recordRecentTools, enabled = !busy,
                        modifier = Modifier.semantics { contentDescription = context.getString(R.string.screen_record_recent_tools) },
                        onCheckedChange = { value -> update({ store.setRecordRecentTools(value) }) }) },
                )
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                TextButton(enabled = !busy && store.recent.isNotEmpty(), onClick = { confirmClear = true }) {
                    Text(context.getString(R.string.screen_clear_recent_history))
                }
                feedback?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                Text(context.getString(R.string.screen_tool_history_saved_results_favorites_and_active_timers_are_separa),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(enabled = !busy, onClick = { open = false }) { Text(context.getString(R.string.screen_done)) } },
    )
    if (confirmClear) AlertDialog(
        onDismissRequest = { if (!busy) confirmClear = false },
        title = { Text(context.getString(R.string.screen_clear_recent_history_2)) },
        text = { Text(context.getString(R.string.screen_remove_the_list_of_recently_opened_tools_from_this_device_this_ca)) },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                confirmClear = false
                update({ store.clear() }, context.getString(R.string.screen_recent_history_cleared))
            }) { Text(context.getString(R.string.screen_clear)) }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = { confirmClear = false }) { Text(context.getString(R.string.screen_cancel)) } },
    )
}
