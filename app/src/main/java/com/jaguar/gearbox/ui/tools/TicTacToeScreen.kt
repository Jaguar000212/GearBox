package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.jaguar.gearbox.R
import androidx.compose.ui.platform.LocalDensity
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
        winner != null -> stringResource(R.string.tic_tac_toe_winner, winner)
        boardFull -> stringResource(R.string.tic_tac_toe_draw)
        else -> stringResource(R.string.tic_tac_toe_turn, currentPlayer)
    }

    ToolScaffold(
        title = stringResource(R.string.tic_tac_toe_title),
        icon = Tools.byRoute(Tools.ROUTE_TIC_TAC_TOE)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        Text(
            text = statusText,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        )

        Spacer(Modifier.height(16.dp))
        // A non-scrolling square board belongs to the screen's single vertical scroll container.
        // The previous fixed-height nested grid clipped cells on wider screens.
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            BoxWithConstraints(Modifier.widthIn(max = 420.dp).fillMaxWidth()) {
                val symbolLineHeight = with(LocalDensity.current) { MaterialTheme.typography.headlineLarge.lineHeight.toDp() }
                val minimumCellHeight = maxOf(48.dp, symbolLineHeight + 24.dp)
                // Remain square normally, but grow vertically rather than clip large-font symbols.
                val boardHeight = maxOf(maxWidth, minimumCellHeight * 3 + 12.dp)
                Column(
                    modifier = Modifier.fillMaxWidth().height(boardHeight),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    repeat(3) { row ->
                        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            repeat(3) { column ->
                                val index = row * 3 + column
                                val isWinningCell = winningCells?.contains(index) == true
                                val canPlay = cells[index] == null && winner == null && !boardFull
                                val position = stringResource(R.string.tic_tac_toe_cell, row + 1, column + 1)
                                val mark = cells[index] ?: stringResource(R.string.tic_tac_toe_empty)
                                val cellState = if (isWinningCell) stringResource(R.string.tic_tac_toe_winning_cell, mark) else mark
                                Card(
                                    enabled = canPlay,
                                    onClick = {
                                        if (board[index].isEmpty() && winningLine(board.map { it.ifEmpty { null } }) == null) {
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
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        disabledContainerColor = if (isWinningCell) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    ),
                                    modifier = Modifier.weight(1f).fillMaxSize().testTag("tic_cell_$index")
                                        .semantics {
                                            role = Role.Button
                                            contentDescription = position
                                            stateDescription = cellState
                                        },
                                ) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = cells[index] ?: "",
                                            style = MaterialTheme.typography.headlineLarge,
                                            modifier = Modifier.clearAndSetSemantics { },
                                            textAlign = TextAlign.Center,
                                            color = if (isWinningCell) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.tic_tac_toe_tally, xWins, oWins, draws),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { newRound() }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Text(stringResource(R.string.tic_tac_toe_new_round))
        }
    }
}
