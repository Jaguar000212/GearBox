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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.ToolScaffold

/** A single competitor tracked on the scoreboard. */
private data class Player(val name: String, val score: Int)

/**
 * Saver that serialises the player list to a flat list of [name, score, name, score, ...] so the
 * scoreboard survives configuration changes and process death.
 */
private val playersSaver: Saver<SnapshotStateList<Player>, List<Any>> = Saver(
    save = { list -> list.flatMap { listOf(it.name, it.score) } },
    restore = { flat ->
        flat.chunked(2)
            .map { Player(it[0] as String, it[1] as Int) }
            .toMutableStateList()
    },
)

@Composable
fun ScoreboardScreen(onNavigateBack: () -> Unit) {
    val players = rememberSaveable(saver = playersSaver) { emptyList<Player>().toMutableStateList() }
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
                onClick = {
                    val name = newName.trim()
                    if (name.isNotEmpty()) {
                        players.add(Player(name, 0))
                        newName = ""
                    }
                },
            ) {
                Icon(Icons.Filled.PersonAdd, contentDescription = "Add player")
            }
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
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = player.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = player.score.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    }
                    IconButton(onClick = { players[index] = player.copy(score = player.score - 1) }) {
                        Icon(Icons.Filled.Remove, contentDescription = "Decrease score")
                    }
                    IconButton(onClick = { players[index] = player.copy(score = player.score + 1) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Increase score")
                    }
                    IconButton(onClick = { players.removeAt(index) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove player")
                    }
                }
            }
        }

        if (players.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        for (i in players.indices) {
                            players[i] = players[i].copy(score = 0)
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Text(" Reset scores")
                }
                OutlinedButton(
                    onClick = { players.clear() },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Text(" Clear all")
                }
            }
        }
    }
}
