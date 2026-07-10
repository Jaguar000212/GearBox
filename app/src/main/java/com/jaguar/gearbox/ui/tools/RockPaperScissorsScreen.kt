package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.ToolScaffold
import kotlin.random.Random

private enum class Move(val label: String, val emoji: String) {
    ROCK("Rock", "✊"),
    PAPER("Paper", "✋"),
    SCISSORS("Scissors", "✌"),
}

private fun Move.beats(other: Move): Boolean = when (this) {
    Move.ROCK -> other == Move.SCISSORS
    Move.PAPER -> other == Move.ROCK
    Move.SCISSORS -> other == Move.PAPER
}

@Composable
fun RockPaperScissorsScreen(onNavigateBack: () -> Unit) {
    var playerMove by rememberSaveable { mutableStateOf<String?>(null) }
    var computerMove by rememberSaveable { mutableStateOf<String?>(null) }
    var resultText by rememberSaveable { mutableStateOf("Choose your move") }
    var wins by rememberSaveable { mutableStateOf(0) }
    var losses by rememberSaveable { mutableStateOf(0) }
    var draws by rememberSaveable { mutableStateOf(0) }

    ToolScaffold(
        title = "Rock Paper Scissors",
        icon = Tools.byRoute(Tools.ROUTE_RPS)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(all = 16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    MoveDisplay("You", playerMove)
                    MoveDisplay("CPU", computerMove)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = resultText,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Move.entries.forEach { move ->
                Button(
                    onClick = {
                        val computer = Move.entries.random()
                        playerMove = move.name
                        computerMove = computer.name
                        resultText = when {
                            move == computer -> {
                                draws++
                                "Draw"
                            }
                            move.beats(computer) -> {
                                wins++
                                "You win!"
                            }
                            else -> {
                                losses++
                                "You lose"
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) { Text(move.emoji) }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = "Wins: $wins   Losses: $losses   Draws: $draws",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MoveDisplay(label: String, moveName: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(
            text = moveName?.let { Move.valueOf(it).emoji } ?: "?",
            style = MaterialTheme.typography.displaySmall,
        )
    }
}
