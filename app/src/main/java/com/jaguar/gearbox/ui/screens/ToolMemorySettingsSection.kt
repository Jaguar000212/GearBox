package com.jaguar.gearbox.ui.screens

import com.jaguar.gearbox.data.localizedInputLabel
import com.jaguar.gearbox.R
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
    Tools.ROUTE_TEXT_TOOLS to R.string.screen_tool_text,
    Tools.ROUTE_QR_GENERATOR to R.string.screen_tool_qr,
    Tools.ROUTE_HASH_GENERATOR to R.string.screen_tool_hash,
    "tool/text_diff" to R.string.screen_tool_text_diff,
    "tool/decision_matrix" to R.string.screen_tool_decision_matrix,
    "tool/bill_splitter" to R.string.screen_tool_bill_splitter,
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
                } else feedback = context.getString(R.string.screen_could_not_save_preferences_please_try_again)
            } finally { busy = false }
        }
    }

    Text(context.getString(R.string.screen_local_data), style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp))
    ListItem(
        headlineContent = { Text(context.getString(R.string.screen_tool_memory_history)) },
        supportingContent = { Text(if (settings.historyEnabled)
            context.getString(R.string.screen_history_on_up_to_1_s_entries_per_tool, settings.historyLimit)
            else context.getString(R.string.screen_history_off_choose_what_your_tools_remember)) },
        modifier = Modifier.clickable(enabled = !busy) { limitText = settings.historyLimit.toString(); open = true },
    )
    if (open) {
        AlertDialog(
            onDismissRequest = { if (!busy) open = false },
            title = { Text(context.getString(R.string.screen_tool_memory_history)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(context.getString(R.string.screen_new_history_and_remembered_modes_results_stay_in_app_private_stor),
                        style = MaterialTheme.typography.bodyMedium)
                    MemoryToggle(context.getString(R.string.screen_remember_tool_modes), context.getString(R.string.screen_reuse_supported_tools_last_selected_options),
                        settings.rememberModes, !busy) { save(settings.copy(rememberModes = it)) }
                    MemoryToggle(context.getString(R.string.screen_remember_last_result), context.getString(R.string.screen_recalculate_supported_tools_from_saved_inputs_random_tools_restor),
                        settings.rememberResults, !busy) { save(settings.copy(rememberResults = it)) }
                    HorizontalDivider()
                    MemoryToggle(context.getString(R.string.screen_remember_game_sessions_and_participants),
                        context.getString(R.string.screen_resume_tambola_rps_tic_tac_toe_tallies_team_participants_and_scor),
                        settings.rememberGameSessions, !busy) { save(settings.copy(rememberGameSessions = it)) }
                    Text(context.getString(R.string.screen_existing_saved_games_are_kept_when_this_is_off_re_enabling_can_re),
                        style = MaterialTheme.typography.bodySmall)
                    TextButton(enabled = !busy, onClick = { clearTarget = "game sessions" }) { Text(context.getString(R.string.screen_clear_saved_game_sessions)) }
                    HorizontalDivider()
                    MemoryToggle(context.getString(R.string.screen_keep_tool_history), context.getString(R.string.screen_save_completed_inputs_and_results_in_supported_tools_turning_this),
                        settings.historyEnabled, !busy) { save(settings.copy(historyEnabled = it)) }
                    HorizontalDivider()
                    Text(context.getString(R.string.screen_text_qr_and_hash_privacy), style = MaterialTheme.typography.titleSmall)
                    Text(context.getString(R.string.screen_these_tools_can_contain_secrets_or_personal_information_automatic),
                        style = MaterialTheme.typography.bodySmall)
                    Text(context.getString(R.string.screen_turning_these_choices_off_retains_existing_records_without_loadin),
                        style = MaterialTheme.typography.bodySmall)
                    privateInputTools.forEach { (route, nameRes) ->
                        val name = context.getString(nameRes)
                        val policy = settings.automaticMemoryFor(route)
                        val counts = savedCounts[route] ?: (0 to 0)
                        Text(name, style = MaterialTheme.typography.titleSmall)
                        MemoryToggle(context.getString(R.string.screen_1_s_automatic_history, name), context.getString(R.string.screen_allow_completed_inputs_and_outputs_to_be_saved_when_keep_tool_his),
                            policy.historyEnabled, !busy) { value ->
                            save(settings.copy(automaticMemory = settings.automaticMemory +
                                (route to policy.copy(historyEnabled = value))))
                        }
                        MemoryToggle(context.getString(R.string.screen_1_s_remember_last_result, name), context.getString(R.string.screen_allow_inputs_and_output_to_be_retained_for_later_use_when_remembe),
                            policy.rememberResults, !busy) { value ->
                            save(settings.copy(automaticMemory = settings.automaticMemory +
                                (route to policy.copy(rememberResults = value))))
                        }
                        Text(context.getString(R.string.screen_memory_counts, context.resources.getQuantityString(R.plurals.screen_history_count, counts.first, counts.first), context.resources.getQuantityString(R.plurals.screen_result_count, counts.second, counts.second)), style = MaterialTheme.typography.bodySmall)
                        TextButton(enabled = !busy, onClick = {
                            busy = true
                            scope.launch {
                                try {
                                    reviewEntries = withContext(Dispatchers.IO) {
                                        buildList {
                                            store.rememberedResult(route)?.let { add(context.getString(R.string.screen_remembered_result) to it) }
                                            store.history(route).forEachIndexed { index, entry -> add(context.getString(R.string.screen_history_1_s, index + 1) to entry) }
                                        }
                                    }
                                    reviewTitle = name
                                } finally { busy = false }
                            }
                        }) { Text(context.getString(R.string.screen_review_1_s_saved_data, name)) }
                        TextButton(enabled = !busy && counts.first > 0,
                            onClick = { clearTarget = "$route|history" }) { Text(context.getString(R.string.screen_clear_1_s_history, name)) }
                        TextButton(enabled = !busy && counts.second > 0,
                            onClick = { clearTarget = "$route|result" }) { Text(context.getString(R.string.screen_clear_1_s_remembered_result, name)) }
                    }
                    HorizontalDivider()
                    OutlinedTextField(
                        value = limitText,
                        onValueChange = { if (it.length <= 3 && it.all(Char::isDigit)) limitText = it },
                        label = { Text(context.getString(R.string.screen_entries_per_tool)) },
                        supportingText = { Text(context.getString(R.string.screen_choose_1_100_lowering_the_limit_removes_the_oldest_entries)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val limit = limitText.toIntOrNull()
                    TextButton(enabled = !busy && limit != null && limit in 1..100 && limit != settings.historyLimit,
                        onClick = { limit?.let { save(settings.copy(historyLimit = it)) } }) {
                        Text(context.getString(R.string.screen_apply_history_limit))
                    }
                    TextButton(enabled = !busy, onClick = { clearTarget = "history" }) { Text(context.getString(R.string.screen_clear_all_tool_history)) }
                    TextButton(enabled = !busy, onClick = { clearTarget = "modes" }) { Text(context.getString(R.string.screen_clear_remembered_modes)) }
                    TextButton(enabled = !busy, onClick = { clearTarget = "results" }) { Text(context.getString(R.string.screen_clear_remembered_results)) }
                    Text(context.getString(R.string.screen_turning_off_mode_or_result_memory_stops_using_it_without_deleting),
                        style = MaterialTheme.typography.bodySmall)
                    feedback?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    Text(context.getString(R.string.screen_these_choices_do_not_reset_active_timers_scores_or_saved_favorite),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { TextButton(enabled = !busy, onClick = { open = false }) { Text(context.getString(R.string.screen_done)) } },
        )
    }
    reviewTitle?.let { name ->
        AlertDialog(
            onDismissRequest = { reviewTitle = null; reviewEntries = emptyList() },
            title = { Text(context.getString(R.string.screen_1_s_saved_data, name)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (reviewEntries.isEmpty()) Text(context.getString(R.string.screen_no_automatic_history_or_remembered_result_is_stored_for_this_tool))
                    reviewEntries.forEach { (label, entry) ->
                        Text(label, style = MaterialTheme.typography.titleSmall)
                        Text(entry.inputs.entries.joinToString("\n") { (key, value) -> "${localizedInputLabel(context, key)}: $value" })
                        Text(entry.output)
                        HorizontalDivider()
                    }
                }
            },
            confirmButton = { TextButton(onClick = { reviewTitle = null; reviewEntries = emptyList() }) { Text(context.getString(R.string.screen_close_review)) } },
        )
    }
    clearTarget?.let { target ->
        val route = target.substringBefore('|').takeIf { candidate -> privateInputTools.any { it.first == candidate } }
        val name = privateInputTools.firstOrNull { it.first == route }?.second?.let { context.getString(it) }
        val kind = target.substringAfter('|')
        val targetName = context.getString(when (target) {
            "game sessions" -> R.string.screen_game_sessions
            "modes" -> R.string.screen_modes
            "results" -> R.string.screen_results
            else -> R.string.screen_history
        })
        val targetLabel = if (route != null) context.getString(if (kind == "history") R.string.screen_named_history else R.string.screen_named_result, name) else context.getString(R.string.screen_remembered_target, targetName)
        AlertDialog(
            onDismissRequest = { clearTarget = null },
            title = { Text(context.getString(R.string.screen_clear_1_s, targetLabel)) },
            text = { Text(if (route != null)
                context.getString(R.string.screen_remove_only_1_s_from_this_device_this_cannot_be_undone_manual_wor, targetLabel)
                else if (target == "game sessions")
                context.getString(R.string.screen_remove_saved_tambola_numbers_rps_tallies_team_participants_and_bo)
                else context.getString(R.string.screen_this_removes_saved_1_s_from_every_supported_tool_on_this_device_i, targetName)) },
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
                        feedback = if (success) context.getString(R.string.screen_1_s_cleared, targetLabel) else context.getString(R.string.screen_could_not_clear_1_s_please_try_again, targetLabel)
                    } finally { busy = false }
                }
            }) { Text(context.getString(R.string.screen_clear)) } },
            dismissButton = { TextButton(enabled = !busy, onClick = { clearTarget = null }) { Text(context.getString(R.string.screen_cancel)) } },
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
