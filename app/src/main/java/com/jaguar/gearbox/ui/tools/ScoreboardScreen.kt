package com.jaguar.gearbox.ui.tools

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

    ToolScaffold(
        title = "Scoreboard",
        icon = Tools.byRoute(Tools.ROUTE_SCOREBOARD)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
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
                "Some saved entries could not be read. Their original data has been retained for recovery; avoid further edits until recovered.",
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
                else -> "Remove every player and score?"
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
}
