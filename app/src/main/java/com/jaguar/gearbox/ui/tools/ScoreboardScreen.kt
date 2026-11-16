package com.jaguar.gearbox.ui.tools

import com.jaguar.gearbox.R

import com.jaguar.gearbox.ui.components.ToolFocusView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.jaguar.gearbox.ui.components.ToolActions
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.rememberCoroutineScope
import com.jaguar.gearbox.data.ScoreSnapshots
import com.jaguar.gearbox.data.ToolMemoryStore
import com.jaguar.gearbox.ui.components.ToolHistoryBinding
import com.jaguar.gearbox.ui.components.rememberToolHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.ScorePlayer
import com.jaguar.gearbox.data.ScoreboardStore
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.ToolScaffold

@Composable
fun ScoreboardScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var pendingAction by remember { mutableStateOf<String?>(null) }
    var pendingRemoval by remember { mutableStateOf<Int?>(null) }
    var limitMessage by remember { mutableStateOf<String?>(null) }
    val store = remember { ScoreboardStore(context, "scoreboard_players") }
    val players = store.players
    var newName by rememberSaveable { mutableStateOf("") }

    var pendingRestore by remember { mutableStateOf<Map<String, String>?>(null) }
    var historyNotice by remember { mutableStateOf<String?>(null) }
    var focusView by rememberSaveable { mutableStateOf(false) }
    var savingSnapshot by remember { mutableStateOf(false) }
    val memory = remember { ToolMemoryStore(context) }
    val snapshotScope = rememberCoroutineScope()
    val snapshot = ScoreSnapshots.scoreboard(players.toList())
    val snapshotSummary = players.mapIndexed { i, p -> "${i + 1}. ${p.name}: ${p.score}" }.joinToString("\n")
    val restoreBinding = rememberToolHistory(Tools.ROUTE_SCOREBOARD, snapshot.orEmpty(), null,
        onRestore = { values ->
            if (store.isReadOnly || ScoreSnapshots.readScoreboard(values) == null) historyNotice = context.getString(R.string.game_this_snapshot_cannot_be_restored_safely)
            else pendingRestore = values.toMap()
        }, restoreLastResult = false)
    val history = ToolHistoryBinding(Tools.ROUTE_SCOREBOARD, restoreBinding.restore, snapshot.orEmpty(),
        snapshotSummary.takeIf { snapshot != null && players.isNotEmpty() })

    ToolScaffold(
        title = context.getString(R.string.game_scoreboard),
        icon = Tools.byRoute(Tools.ROUTE_SCOREBOARD)!!.icon,
        onNavigateBack = onNavigateBack,
        history = history,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(enabled = snapshot != null && players.isNotEmpty() && !savingSnapshot, onClick = {
                val captured = snapshot ?: return@TextButton
                val output = snapshotSummary
                // Capture before dispatch: a later clear/delete invalidates this pending write.
                val generation = memory.historyGeneration(Tools.ROUTE_SCOREBOARD)
                savingSnapshot = true
                snapshotScope.launch {
                    historyNotice = withContext(Dispatchers.IO) {
                        if (!memory.settings().historyEnabled) context.getString(R.string.game_enable_history_in_settings_tool_memory_to_save_snapshots)
                        else if (memory.record(Tools.ROUTE_SCOREBOARD, captured, output, expectedGeneration = generation)) context.getString(R.string.game_snapshot_saved)
                        else context.getString(R.string.game_snapshot_could_not_be_saved_the_data_may_exceed_the_histor)
                    }
                    savingSnapshot = false
                }
            }) { Text(if (savingSnapshot) context.getString(R.string.game_saving) else context.getString(R.string.game_save_snapshot)) }
            TextButton(onClick = { focusView = true }) { Text(context.getString(R.string.game_focus_view)) }
        }
        historyNotice?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text(context.getString(R.string.game_player_team_name)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            FilledTonalIconButton(
                enabled = newName.isNotBlank() && !store.isReadOnly,
                onClick = {
                    val name = newName.trim()
                    if (name.isNotEmpty()) {
                        store.add(ScorePlayer(name, 0))
                        newName = ""
                    }
                },
            ) {
                Icon(Icons.Filled.PersonAdd, contentDescription = context.getString(R.string.game_add_player))
            }
        }

        if (store.hasUnreadableSavedData) {
            Text(
                if (store.isReadOnly) context.getString(R.string.game_saved_data_could_not_be_read_editing_is_disabled_to_protec)
                else context.getString(R.string.game_older_recovery_data_is_retained_separately_removing_player),
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (players.isEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = context.getString(R.string.game_add_a_player_or_team_to_start_keeping_score),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        players.forEachIndexed { index, player ->
            Spacer(Modifier.height(12.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = player.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = player.score.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    }
                    IconButton(enabled = !store.isReadOnly, onClick = {
                        limitMessage = if (store.adjustScore(index, -1)) null else context.getString(R.string.game_score_limit_reached)
                    }) {
                        Icon(Icons.Filled.Remove, contentDescription = context.getString(R.string.game_decrease_score_for_1_s, player.name))
                    }
                    IconButton(enabled = !store.isReadOnly, onClick = {
                        limitMessage = if (store.adjustScore(index, 1)) null else context.getString(R.string.game_score_limit_reached)
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = context.getString(R.string.game_increase_score_for_1_s, player.name))
                    }
                    IconButton(enabled = !store.isReadOnly, onClick = { pendingRemoval = index; pendingAction = "Remove player" }) {
                        Icon(Icons.Filled.Delete, contentDescription = context.getString(R.string.game_remove_1_s, player.name))
                    }
                }
            }
        }

        limitMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (players.isNotEmpty()) {
            val summary = players.mapIndexed { i, p -> "${i + 1}. ${p.name}: ${p.score}" }.joinToString("\n")
            Spacer(Modifier.height(12.dp))
            ToolActions(
                onCopy = { context.copyToClipboard(context.getString(R.string.game_score_summary), summary) },
                onShare = {
                    context.shareText(summary)
                },
            )
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    enabled = !store.isReadOnly,
                    onClick = { pendingAction = "Reset scores" },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Text(context.getString(R.string.game_reset_scores))
                }
                OutlinedButton(
                    enabled = !store.isReadOnly,
                    onClick = { pendingAction = "Clear all" },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Text(context.getString(R.string.game_clear_all))
                }
            }
        }
    }
    pendingAction?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingAction = null; pendingRemoval = null },
            title = { Text(context.getString(when (action) { "Remove player" -> R.string.game_remove_player_question; "Reset scores" -> R.string.game_reset_scores_question; else -> R.string.game_clear_all_question })) },
            text = { Text(when (action) {
                "Remove player" -> context.getString(R.string.game_remove_this_player_and_their_score)
                "Reset scores" -> context.getString(R.string.game_set_every_score_to_zero_player_names_will_be_kept)
                else -> if (store.hasRetainedLegacyData) context.getString(R.string.game_remove_every_current_player_and_score_older_recovery_data) else context.getString(R.string.game_remove_every_player_and_score)
            }) },
            confirmButton = { TextButton(onClick = {
                when (action) {
                    "Remove player" -> pendingRemoval?.let(store::removeAt)
                    "Reset scores" -> store.resetScores()
                    else -> store.clear()
                }
                pendingAction = null
                pendingRemoval = null
                limitMessage = null
            }) { Text(context.getString(R.string.game_confirm)) } },
            dismissButton = { TextButton(onClick = { pendingAction = null; pendingRemoval = null }) { Text(context.getString(R.string.game_cancel)) } },
        )
    }
    pendingRestore?.let { values ->
        AlertDialog(onDismissRequest = { pendingRestore = null }, title = { Text(context.getString(R.string.game_replace_current_scoreboard)) },
            text = { Text(context.getString(R.string.game_this_replaces_all_current_player_names_and_scores_with_the)) },
            confirmButton = { TextButton(onClick = {
                historyNotice = if (store.restoreSnapshot(values)) context.getString(R.string.game_scoreboard_restored) else context.getString(R.string.game_could_not_restore_current_scores_were_not_replaced)
                pendingRestore = null
            }) { Text(context.getString(R.string.game_replace_scoreboard)) } },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text(context.getString(R.string.game_cancel)) } })
    }
    if (focusView) ToolFocusView(context.getString(R.string.game_scoreboard), onExit = { focusView = false }) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            players.forEach { player ->
                Text(player.name, style = MaterialTheme.typography.titleLarge)
                Text(player.score.toString(), style = MaterialTheme.typography.displayMedium)
            }
        }
    }

}
