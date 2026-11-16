package com.jaguar.gearbox.ui.tools

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
            if (store.isReadOnly || ScoreSnapshots.readScoreboard(values) == null) historyNotice = "This snapshot cannot be restored safely."
            else pendingRestore = values.toMap()
        }, restoreLastResult = false)
    val history = ToolHistoryBinding(Tools.ROUTE_SCOREBOARD, restoreBinding.restore, snapshot.orEmpty(),
        snapshotSummary.takeIf { snapshot != null && players.isNotEmpty() })

    ToolScaffold(
        title = "Scoreboard",
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
                        if (!memory.settings().historyEnabled) "Enable history in Settings → Tool memory to save snapshots."
                        else if (memory.record(Tools.ROUTE_SCOREBOARD, captured, output, expectedGeneration = generation)) "Snapshot saved."
                        else "Snapshot could not be saved. The data may exceed the history size limit."
                    }
                    savingSnapshot = false
                }
            }) { Text(if (savingSnapshot) "Saving…" else "Save snapshot") }
            TextButton(onClick = { focusView = true }) { Text("Focus view") }
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
                label = { Text("Player / team name") },
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
                Icon(Icons.Filled.PersonAdd, contentDescription = "Add player")
            }
        }

        if (store.hasUnreadableSavedData) {
            Text(
                if (store.isReadOnly) "Saved data could not be read. Editing is disabled to protect it."
                else "Older recovery data is retained separately. Removing players, resetting scores or clearing this scoreboard will not erase that recovery data.",
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (players.isEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Add a player or team to start keeping score.",
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
                        limitMessage = if (store.adjustScore(index, -1)) null else "Score limit reached."
                    }) {
                        Icon(Icons.Filled.Remove, contentDescription = "Decrease score for ${player.name}")
                    }
                    IconButton(enabled = !store.isReadOnly, onClick = {
                        limitMessage = if (store.adjustScore(index, 1)) null else "Score limit reached."
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "Increase score for ${player.name}")
                    }
                    IconButton(enabled = !store.isReadOnly, onClick = { pendingRemoval = index; pendingAction = "Remove player" }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove ${player.name}")
                    }
                }
            }
        }

        limitMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (players.isNotEmpty()) {
            val summary = players.mapIndexed { i, p -> "${i + 1}. ${p.name}: ${p.score}" }.joinToString("\n")
            Spacer(Modifier.height(12.dp))
            ToolActions(
                onCopy = { context.copyToClipboard("Score summary", summary) },
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
                    Text(" Reset scores")
                }
                OutlinedButton(
                    enabled = !store.isReadOnly,
                    onClick = { pendingAction = "Clear all" },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Text(" Clear all")
                }
            }
        }
    }
    pendingAction?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingAction = null; pendingRemoval = null },
            title = { Text("$action?") },
            text = { Text(when (action) {
                "Remove player" -> "Remove this player and their score?"
                "Reset scores" -> "Set every score to zero? Player names will be kept."
                else -> if (store.hasRetainedLegacyData) "Remove every current player and score? Older recovery data will remain stored." else "Remove every player and score?"
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
            }) { Text("Confirm") } },
            dismissButton = { TextButton(onClick = { pendingAction = null; pendingRemoval = null }) { Text("Cancel") } },
        )
    }
    pendingRestore?.let { values ->
        AlertDialog(onDismissRequest = { pendingRestore = null }, title = { Text("Replace current scoreboard?") },
            text = { Text("This replaces all current player names and scores with the selected snapshot. Older recovery data is kept. Save a snapshot of the current game first if you need it.") },
            confirmButton = { TextButton(onClick = {
                historyNotice = if (store.restoreSnapshot(values)) "Scoreboard restored." else "Could not restore. Current scores were not replaced."
                pendingRestore = null
            }) { Text("Replace scoreboard") } },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("Cancel") } })
    }
    if (focusView) ToolFocusView("Scoreboard", onExit = { focusView = false }) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            players.forEach { player ->
                Text(player.name, style = MaterialTheme.typography.titleLarge)
                Text(player.score.toString(), style = MaterialTheme.typography.displayMedium)
            }
        }
    }

}
