package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.SimplePrefsStore
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.logic.isBoardFull
import com.jaguar.gearbox.logic.winningLine
import com.jaguar.gearbox.ui.components.StringListSaver
import com.jaguar.gearbox.ui.components.ToolScaffold

private const val KEY_X_WINS = "tic_tac_toe.x_wins"
private const val KEY_O_WINS = "tic_tac_toe.o_wins"
private const val KEY_DRAWS = "tic_tac_toe.draws"

@Composable
fun TicTacToeScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { SimplePrefsStore(context) }
    var board by rememberSaveable(stateSaver = StringListSaver) {
        mutableStateOf(List(9) { "" })
    }
    var currentPlayer by rememberSaveable { mutableStateOf("X") }
    var xWins by rememberSaveable { mutableStateOf(store.getInt(KEY_X_WINS, 0)) }
    var oWins by rememberSaveable { mutableStateOf(store.getInt(KEY_O_WINS, 0)) }
    var draws by rememberSaveable { mutableStateOf(store.getInt(KEY_DRAWS, 0)) }

    val cells = board.map { it.ifEmpty { null } }
    val winningCells = winningLine(cells)
    val winner = winningCells?.let { cells[it[0]] }
    val boardFull = isBoardFull(cells)

    fun persistTally() {
        store.edit {
            putInt(KEY_X_WINS, xWins)
            putInt(KEY_O_WINS, oWins)
            putInt(KEY_DRAWS, draws)
        }
    }

    fun newRound() {
        board = List(9) { "" }
        currentPlayer = "X"
    }

    val statusText = when {
        winner != null -> "$winner wins!"
        boardFull -> "Draw!"
        else -> "$currentPlayer's turn"
    }

    ToolScaffold(
        title = "Tic-tac-toe",
        icon = Tools.byRoute(Tools.ROUTE_TIC_TAC_TOE)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        Text(
            text = statusText,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(9) { index ->
                val isWinningCell = winningCells?.contains(index) == true
                Card(
                    onClick = {
                        if (cells[index] == null && winner == null) {
                            board = board.toMutableList().also { it[index] = currentPlayer }
                            val updatedCells = board.map { c -> c.ifEmpty { null } }
                            val newWinner = winningLine(updatedCells)?.let { updatedCells[it[0]] }
                            when {
                                newWinner == "X" -> { xWins++; persistTally() }
                                newWinner == "O" -> { oWins++; persistTally() }
                                isBoardFull(updatedCells) -> { draws++; persistTally() }
                            }
                            currentPlayer = if (currentPlayer == "X") "O" else "X"
                        }
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isWinningCell) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                ) {
                    Text(
                        text = cells[index] ?: "",
                        style = MaterialTheme.typography.displaySmall,
                        textAlign = TextAlign.Center,
                        color = if (isWinningCell) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "X wins: $xWins   O wins: $oWins   Draws: $draws",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { newRound() }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Text(" New round")
        }
    }
}
