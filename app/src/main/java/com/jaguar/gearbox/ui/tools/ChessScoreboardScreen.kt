package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.ChessMatch
import com.jaguar.gearbox.data.ChessMatchStore
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.ToolScaffold

@Composable
fun ChessScoreboardScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { ChessMatchStore(context) }
    val initial = remember { store.load() }

    var whiteName by rememberSaveable { mutableStateOf(initial.whiteName) }
    var blackName by rememberSaveable { mutableStateOf(initial.blackName) }
    var whiteScore by rememberSaveable { mutableFloatStateOf(initial.whiteScore) }
    var blackScore by rememberSaveable { mutableFloatStateOf(initial.blackScore) }
    var gamesPlayed by rememberSaveable { mutableIntStateOf(initial.gamesPlayed) }

    fun persist() {
        store.save(ChessMatch(whiteName, blackName, whiteScore, blackScore, gamesPlayed))
    }

    ToolScaffold(
        title = "Chess Scoreboard",
        icon = Tools.byRoute(Tools.ROUTE_CHESS_SCOREBOARD)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = whiteName,
                onValueChange = { whiteName = it; persist() },
                label = { Text("White player") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = blackName,
                onValueChange = { blackName = it; persist() },
                label = { Text("Black player") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(20.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ScoreColumn(whiteName, whiteScore)
                Column {
                    Text(
                        text = "Games",
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = gamesPlayed.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                ScoreColumn(blackName, blackScore)
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { whiteScore += 1f; gamesPlayed++; persist() },
                modifier = Modifier.weight(1f),
            ) { Text("$whiteName wins") }
            OutlinedButton(
                onClick = { whiteScore += 0.5f; blackScore += 0.5f; gamesPlayed++; persist() },
                modifier = Modifier.weight(1f),
            ) { Text("Draw") }
            OutlinedButton(
                onClick = { blackScore += 1f; gamesPlayed++; persist() },
                modifier = Modifier.weight(1f),
            ) { Text("$blackName wins") }
        }

        Spacer(Modifier.height(20.dp))
        OutlinedButton(
            onClick = {
                whiteScore = 0f
                blackScore = 0f
                gamesPlayed = 0
                persist()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Text(" Reset match")
        }
    }
}

@Composable
private fun ScoreColumn(name: String, score: Float) {
    Column {
        Text(
            text = name,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = if (score == score.toInt().toFloat()) score.toInt().toString() else score.toString(),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
