package com.jaguar.gearbox.ui.tools

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.ToolMemoryStore
import com.jaguar.gearbox.data.SimplePrefsStore
import com.jaguar.gearbox.data.localizedSummary
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.logic.isBoardFull
import com.jaguar.gearbox.logic.winningLine
import com.jaguar.gearbox.ui.components.StringListSaver
import com.jaguar.gearbox.ui.components.ToolScaffold
import com.jaguar.gearbox.ui.components.rememberToolHistory
import com.jaguar.gearbox.logic.TicTacToeSnapshot
import com.jaguar.gearbox.logic.parseTicTacToeSnapshot

private const val KEY_X_WINS = "tic_tac_toe.x_wins"
private const val KEY_O_WINS = "tic_tac_toe.o_wins"
private const val KEY_DRAWS = "tic_tac_toe.draws"

@Composable
fun TicTacToeScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { SimplePrefsStore(context, gameSession = true) }
    val memory = remember { ToolMemoryStore(context) }
    var board by rememberSaveable(stateSaver = StringListSaver) {
        mutableStateOf(List(9) { "" })
    }
    var currentPlayer by rememberSaveable { mutableStateOf("X") }
    var xWins by rememberSaveable { mutableStateOf(store.getInt(KEY_X_WINS, 0)) }
    var oWins by rememberSaveable { mutableStateOf(store.getInt(KEY_O_WINS, 0)) }
    var draws by rememberSaveable { mutableStateOf(store.getInt(KEY_DRAWS, 0)) }
    // Capture is a one-shot UI event, not restorable state: rotation must not re-record a deleted snapshot.
    var capturedBoard by remember { mutableStateOf<String?>(null) }
    var captureId by remember { mutableStateOf("") }
    var captureGeneration by remember { mutableStateOf<Long?>(null) }
    var pendingRestore by rememberSaveable { mutableStateOf<String?>(null) }
    var importError by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingAction by rememberSaveable { mutableStateOf<String?>(null) }

    val cells = board.map { it.ifEmpty { null } }
    val winningCells = winningLine(cells)
    val winner = winningCells?.let { cells[it[0]] }
    val boardFull = isBoardFull(cells)
    val roundInProgress = board.any { it.isNotEmpty() } && winner == null && !boardFull

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

    fun requestBack() {
        if (roundInProgress) pendingAction = "leave" else onNavigateBack()
    }
    BackHandler(enabled = roundInProgress && pendingAction == null && pendingRestore == null) { requestBack() }

    if (pendingAction != null) {
        val leaving = pendingAction == "leave"
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            title = { Text(stringResource(R.string.ttt_abandon_title)) },
            text = { Text(stringResource(R.string.ttt_abandon_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingAction = null
                        if (leaving) onNavigateBack() else newRound()
                    },
                    modifier = Modifier.testTag("tic_confirm_discard"),
                ) {
                    Text(stringResource(if (leaving) R.string.ttt_leave else R.string.tic_tac_toe_new_round))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingAction = null }) {
                    Text(stringResource(R.string.ttt_keep_playing))
                }
            },
        )
    }

    fun applySnapshot(snapshot: TicTacToeSnapshot) {
        board = snapshot.board
        currentPlayer = snapshot.nextPlayer
        capturedBoard = null
        importError = null
        // Lifetime scores are deliberately unchanged: importing a winner is not a new win.
    }
    val captured = capturedBoard?.let { parseTicTacToeSnapshot(mapOf("board" to it)) }
    val history = rememberToolHistory(
        route = Tools.ROUTE_TIC_TAC_TOE,
        inputs = captured?.inputs()?.plus("_run" to captureId) ?: emptyMap(),
        output = captured?.localizedSummary(context),
        recordingDelayMillis = 0L,
        expectedHistoryGeneration = captureGeneration,
        restoreLastResult = false,
        onRestore = { saved ->
            val snapshot = parseTicTacToeSnapshot(saved)
            if (snapshot == null) importError = context.getString(R.string.game_this_snapshot_is_not_a_valid_game_position_the_current_rou)
            else if (board.any { it.isNotEmpty() }) pendingRestore = snapshot.inputs().getValue("board")
            else applySnapshot(snapshot)
        },
    )
    if (pendingRestore != null) AlertDialog(
        onDismissRequest = { pendingRestore = null },
        title = { Text(context.getString(R.string.game_restore_game_snapshot)) },
        text = { Text(context.getString(R.string.game_this_replaces_the_current_board_your_lifetime_tally_will_n)) },
        confirmButton = { TextButton(onClick = {
            pendingRestore?.let { parseTicTacToeSnapshot(mapOf("board" to it)) }?.let { applySnapshot(it) }
            pendingRestore = null
        }) { Text(context.getString(R.string.game_restore)) } },
        dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text(context.getString(R.string.game_keep_current_round)) } },
    )

    ToolScaffold(
        title = stringResource(R.string.tic_tac_toe_title),
        icon = Tools.byRoute(Tools.ROUTE_TIC_TAC_TOE)!!.icon,
        onNavigateBack = { requestBack() },
        history = history,
    ) {
        importError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.ttt_local_players), style = MaterialTheme.typography.labelLarge)
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                )
                Text(
                    text = stringResource(if (winner != null || boardFull) R.string.ttt_round_complete else R.string.ttt_how_to_play),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        // A non-scrolling square board belongs to the screen's single vertical scroll container.
        // The previous fixed-height nested grid clipped cells on wider screens.
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            BoxWithConstraints(Modifier.widthIn(max = 420.dp).fillMaxWidth()) {
                val symbolLineHeight = with(LocalDensity.current) { MaterialTheme.typography.headlineLarge.lineHeight.toDp() }
                val minimumCellHeight = maxOf(48.dp, symbolLineHeight + 40.dp)
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
                                    border = if (isWinningCell) BorderStroke(3.dp, MaterialTheme.colorScheme.onPrimary) else null,
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
                                        if (isWinningCell) {
                                            Icon(
                                                Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                                            )
                                        }
                                        Text(
                                            text = cells[index] ?: "",
                                            style = MaterialTheme.typography.headlineLarge,
                                            fontWeight = if (isWinningCell) FontWeight.Bold else FontWeight.Normal,
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
        val tallyDescription = stringResource(R.string.tic_tac_toe_tally, xWins, oWins, draws)
        Row(
            modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = tallyDescription },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TallyCard("X", xWins, Modifier.weight(1f))
            TallyCard(stringResource(R.string.ttt_draws_label), draws, Modifier.weight(1f))
            TallyCard("O", oWins, Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = {
            // Fence the explicit event before dispatching its recording effect.
            captureGeneration = memory.historyGeneration(Tools.ROUTE_TIC_TAC_TOE)
            capturedBoard = TicTacToeSnapshot(board).inputs().getValue("board")
            captureId = java.util.UUID.randomUUID().toString()
        }, enabled = board.any { it.isNotEmpty() }, modifier = Modifier.fillMaxWidth()) { Text(context.getString(R.string.game_capture_snapshot)) }
        Text(if (captured == null) context.getString(R.string.game_capture_this_round_for_history_recording_follows_your_hist)
            else context.getString(R.string.game_snapshot_prepared_open_history_to_copy_share_or_keep_resul),
            style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { if (roundInProgress) pendingAction = "restart" else newRound() },
            modifier = Modifier.fillMaxWidth().testTag("tic_new_round"),
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text(stringResource(R.string.tic_tac_toe_new_round))
        }
    }
}


@Composable
private fun TallyCard(label: String, value: Int, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
            Text(value.toString(), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        }
    }
}
