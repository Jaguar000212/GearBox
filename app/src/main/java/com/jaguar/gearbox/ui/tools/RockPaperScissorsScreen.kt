package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.jaguar.gearbox.data.SimplePrefsStore
import com.jaguar.gearbox.data.RandomResultSnapshots
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.ToolScaffold
import com.jaguar.gearbox.ui.components.rememberToolHistory

private const val KEY_WINS = "rps.wins"
private const val KEY_LOSSES = "rps.losses"
private const val KEY_DRAWS = "rps.draws"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RockPaperScissorsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { SimplePrefsStore(context, gameSession = true) }
    var runId by rememberSaveable { mutableStateOf("") }
    var playerMove by rememberSaveable { mutableStateOf<String?>(null) }
    var computerMove by rememberSaveable { mutableStateOf<String?>(null) }
    var resultText by rememberSaveable { mutableStateOf("Choose your move") }
    var wins by rememberSaveable { mutableStateOf(store.getInt(KEY_WINS, 0)) }
    var losses by rememberSaveable { mutableStateOf(store.getInt(KEY_LOSSES, 0)) }
    var draws by rememberSaveable { mutableStateOf(store.getInt(KEY_DRAWS, 0)) }

    var confirmReset by rememberSaveable { mutableStateOf(false) }

    fun persistTally() {
        store.edit {
            putInt(KEY_WINS, wins)
            putInt(KEY_LOSSES, losses)
            putInt(KEY_DRAWS, draws)
        }
    }

    val history = rememberToolHistory(
        route = Tools.ROUTE_RPS,
        inputs = mapOf("playerMove" to (playerMove ?: ""), "_run" to runId, "_cpuMove" to computerMove.orEmpty()),
        output = if (playerMove != null && computerMove != null)
            "You: $playerMove\nCPU: $computerMove\n$resultText" else null,
        recordingDelayMillis = 0L,
        onRestore = { saved ->
            playerMove = RpsMove.entries.firstOrNull { it.name == saved["playerMove"] }?.name
            computerMove = null
            resultText = playerMove?.let { "Previous choice: ${RpsMove.valueOf(it).label}. Tap a move to play." }
                ?: "Choose your move"
            RandomResultSnapshots.rps(saved)?.let { (player, cpu) ->
                playerMove = player; computerMove = cpu
                resultText = rpsOutcome(RpsMove.valueOf(player), RpsMove.valueOf(cpu)).message
                runId = saved["_run"].orEmpty()
                // This is display hydration only: no RNG and no tally update.
            }
        },
    )

    ToolScaffold(
        title = "Rock Paper Scissors",
        icon = Tools.byRoute(Tools.ROUTE_RPS)!!.icon,
        onNavigateBack = onNavigateBack,
        history = history,
    ) {
        Text("Pick a move. The CPU chooses randomly.", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MoveDisplay("You", playerMove)
                    MoveDisplay("CPU", computerMove)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = resultText,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RpsMove.entries.forEach { move ->
                Button(
                    onClick = {
                        runId = java.util.UUID.randomUUID().toString()
                        val computer = RpsMove.entries.random()
                        playerMove = move.name
                        computerMove = computer.name
                        val outcome = rpsOutcome(move, computer)
                        resultText = outcome.message
                        when (outcome) {
                            RpsOutcome.DRAW -> draws++
                            RpsOutcome.WIN -> wins++
                            RpsOutcome.LOSS -> losses++
                        }
                        persistTally()
                    },
                    modifier = Modifier.widthIn(min = 100.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(move.emoji, style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.clearAndSetSemantics { })
                        Text(move.label, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Wins" to wins, "Losses" to losses, "Draws" to draws).forEach { (label, count) ->
                Card(Modifier.widthIn(min = 100.dp)) {
                    Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(count.toString(), style = MaterialTheme.typography.headlineSmall)
                        Text(label, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { confirmReset = true },
            enabled = wins != 0 || losses != 0 || draws != 0,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Text(" Reset tally")
        }
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset tally?") },
            text = { Text("This clears your saved wins, losses, draws and the current round.") },
            confirmButton = {
                TextButton(onClick = {
                    playerMove = null
                    computerMove = null
                    resultText = "Choose your move"
                    wins = 0
                    losses = 0
                    draws = 0
                    persistTally()
                    confirmReset = false
                }) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }

}

@Composable
private fun MoveDisplay(label: String, moveName: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(
            text = moveName?.let { RpsMove.valueOf(it).emoji } ?: "?",
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.clearAndSetSemantics { },
        )
        Text(moveName?.let { RpsMove.valueOf(it).label } ?: "Not chosen", style = MaterialTheme.typography.bodyMedium)
    }
}
