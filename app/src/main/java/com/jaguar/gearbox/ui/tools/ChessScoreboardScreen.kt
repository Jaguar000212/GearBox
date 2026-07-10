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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.ToolScaffold

@Composable
fun ChessScoreboardScreen(onNavigateBack: () -> Unit) {
    var whiteName by rememberSaveable { mutableStateOf("White") }
    var blackName by rememberSaveable { mutableStateOf("Black") }
    var whiteScore by rememberSaveable { mutableFloatStateOf(0f) }
    var blackScore by rememberSaveable { mutableFloatStateOf(0f) }
    var gamesPlayed by rememberSaveable { mutableIntStateOf(0) }

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
                onValueChange = { whiteName = it },
                label = { Text("White player") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = blackName,
                onValueChange = { blackName = it },
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
                onClick = { whiteScore += 1f; gamesPlayed++ },
                modifier = Modifier.weight(1f),
            ) { Text("$whiteName wins") }
            OutlinedButton(
                onClick = { whiteScore += 0.5f; blackScore += 0.5f; gamesPlayed++ },
                modifier = Modifier.weight(1f),
            ) { Text("Draw") }
            OutlinedButton(
                onClick = { blackScore += 1f; gamesPlayed++ },
                modifier = Modifier.weight(1f),
            ) { Text("$blackName wins") }
        }

        Spacer(Modifier.height(20.dp))
        OutlinedButton(
            onClick = {
                whiteScore = 0f
                blackScore = 0f
                gamesPlayed = 0
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
