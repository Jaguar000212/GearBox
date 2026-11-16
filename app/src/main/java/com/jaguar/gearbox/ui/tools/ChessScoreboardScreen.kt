package com.jaguar.gearbox.ui.tools

import com.jaguar.gearbox.R

import com.jaguar.gearbox.ui.components.ToolFocusView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.jaguar.gearbox.ui.components.ToolActions
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.ChessMatch
import com.jaguar.gearbox.data.ChessMatchStore
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.ToolScaffold

@Composable
fun ChessScoreboardScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    val store = remember { ChessMatchStore(context) }
    val initial = remember { store.load() }

    var whiteName by rememberSaveable { mutableStateOf(initial.whiteName) }
    var blackName by rememberSaveable { mutableStateOf(initial.blackName) }
    var whiteScore by rememberSaveable { mutableFloatStateOf(initial.whiteScore) }
    var blackScore by rememberSaveable { mutableFloatStateOf(initial.blackScore) }
    var gamesPlayed by rememberSaveable { mutableIntStateOf(initial.gamesPlayed) }

    // Only the last score-changing action (white/black win or draw) can be undone - a reset
    // clears it, since undoing "back to before the reset" isn't a meaningful action here.
    var lastWhiteDelta by rememberSaveable { mutableFloatStateOf(0f) }
    var lastBlackDelta by rememberSaveable { mutableFloatStateOf(0f) }
    var canUndo by rememberSaveable { mutableStateOf(false) }

    fun persist() {
        store.save(ChessMatch(whiteName, blackName, whiteScore, blackScore, gamesPlayed))
    }

    fun recordResult(whiteDelta: Float, blackDelta: Float) {
        whiteScore += whiteDelta
        blackScore += blackDelta
        gamesPlayed++
        lastWhiteDelta = whiteDelta
        lastBlackDelta = blackDelta
        canUndo = true
        persist()
    }

    var pendingRestore by remember { mutableStateOf<Map<String, String>?>(null) }
    var historyNotice by remember { mutableStateOf<String?>(null) }
    var focusView by rememberSaveable { mutableStateOf(false) }
    var savingSnapshot by remember { mutableStateOf(false) }
    val memory = remember { ToolMemoryStore(context) }
    val snapshotScope = rememberCoroutineScope()
    val snapshot = ScoreSnapshots.chess(ChessMatch(whiteName, blackName, whiteScore, blackScore, gamesPlayed))
    val snapshotSummary = context.getString(R.string.game_chess_match_n_1_s_2_s_n_3_s_4_s_ngames_5_s, whiteName.ifBlank { context.getString(R.string.game_white) }, whiteScore, blackName.ifBlank { context.getString(R.string.game_black) }, blackScore, gamesPlayed)
    val restoreBinding = rememberToolHistory(Tools.ROUTE_CHESS_SCOREBOARD, snapshot.orEmpty(), null,
        onRestore = { values ->
            if (ScoreSnapshots.readChess(values) == null) historyNotice = context.getString(R.string.game_this_snapshot_cannot_be_restored_safely)
            else pendingRestore = values.toMap()
        }, restoreLastResult = false)
    val history = ToolHistoryBinding(Tools.ROUTE_CHESS_SCOREBOARD, restoreBinding.restore, snapshot.orEmpty(),
        snapshotSummary.takeIf { snapshot != null })

    ToolScaffold(
        title = context.getString(R.string.game_chess_scoreboard),
        icon = Tools.byRoute(Tools.ROUTE_CHESS_SCOREBOARD)!!.icon,
        onNavigateBack = onNavigateBack,
        history = history,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(enabled = snapshot != null && !savingSnapshot, onClick = {
                val captured = snapshot ?: return@TextButton
                val output = snapshotSummary
                // Capture before dispatch: a later clear/delete invalidates this pending write.
                val generation = memory.historyGeneration(Tools.ROUTE_CHESS_SCOREBOARD)
                savingSnapshot = true
                snapshotScope.launch {
                    historyNotice = withContext(Dispatchers.IO) {
                        if (!memory.settings().historyEnabled) context.getString(R.string.game_enable_history_in_settings_tool_memory_to_save_snapshots)
                        else if (memory.record(Tools.ROUTE_CHESS_SCOREBOARD, captured, output, expectedGeneration = generation)) context.getString(R.string.game_snapshot_saved)
                        else context.getString(R.string.game_snapshot_could_not_be_saved_the_data_may_exceed_the_histor)
                    }
                    savingSnapshot = false
                }
            }) { Text(if (savingSnapshot) context.getString(R.string.game_saving) else context.getString(R.string.game_save_snapshot)) }
            TextButton(onClick = { focusView = true }) { Text(context.getString(R.string.game_focus_view)) }
        }
        historyNotice?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = whiteName,
                onValueChange = { whiteName = it; persist() },
                label = { Text(context.getString(R.string.game_white_player)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = blackName,
                onValueChange = { blackName = it; persist() },
                label = { Text(context.getString(R.string.game_black_player)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(20.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ScoreColumn(whiteName.ifBlank { context.getString(R.string.game_white) }, whiteScore, Modifier.weight(1f).testTag("white-score"))
                Column(Modifier.weight(0.7f)) {
                    Text(
                        text = context.getString(R.string.game_games),
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
                ScoreColumn(blackName.ifBlank { context.getString(R.string.game_black) }, blackScore, Modifier.weight(1f).testTag("black-score"))
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { recordResult(whiteDelta = 1f, blackDelta = 0f) },
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    context.getString(R.string.game_1_s_wins, whiteName),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
            OutlinedButton(
                onClick = { recordResult(whiteDelta = 0.5f, blackDelta = 0.5f) },
                modifier = Modifier.weight(1f),
            ) { Text(context.getString(R.string.game_draw)) }
            OutlinedButton(
                onClick = { recordResult(whiteDelta = 0f, blackDelta = 1f) },
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    context.getString(R.string.game_1_s_wins, blackName),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = {
                    if (canUndo && gamesPlayed > 0) {
                    whiteScore -= lastWhiteDelta
                    blackScore -= lastBlackDelta
                    gamesPlayed--
                    canUndo = false
                    persist()
                    }
                },
                enabled = canUndo,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null)
                Text(context.getString(R.string.game_undo_last_result))
            }
            OutlinedButton(
                onClick = { confirmReset = true },
                enabled = gamesPlayed > 0,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Text(context.getString(R.string.game_reset_match))
            }
        }
        Spacer(Modifier.height(12.dp))
        val summary = context.getString(R.string.game_chess_match_n_1_s_2_s_n_3_s_4_s_ngames_5_s, whiteName.ifBlank { context.getString(R.string.game_white) }, whiteScore, blackName.ifBlank { context.getString(R.string.game_black) }, blackScore, gamesPlayed)
        ToolActions(
            onCopy = { context.copyToClipboard(context.getString(R.string.game_score_summary), summary) },
            onShare = {
                context.shareText(summary)
            },
        )
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(context.getString(R.string.game_reset_match_78)) },
            text = { Text(context.getString(R.string.game_clear_both_scores_and_the_game_count_player_names_will_be)) },
            confirmButton = { TextButton(onClick = {
                whiteScore = 0f
                blackScore = 0f
                gamesPlayed = 0
                canUndo = false
                persist()
                confirmReset = false
            }) { Text(context.getString(R.string.game_reset_45)) } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(context.getString(R.string.game_cancel)) } },
        )
    }
    pendingRestore?.let { values ->
        AlertDialog(onDismissRequest = { pendingRestore = null }, title = { Text(context.getString(R.string.game_replace_current_match)) },
            text = { Text(context.getString(R.string.game_this_replaces_both_player_names_scores_and_game_count_undo)) },
            confirmButton = { TextButton(onClick = {
                val restored = store.restoreSnapshot(values)
                if (restored != null) {
                    whiteName = restored.whiteName; blackName = restored.blackName
                    whiteScore = restored.whiteScore; blackScore = restored.blackScore; gamesPlayed = restored.gamesPlayed
                    canUndo = false; lastWhiteDelta = 0f; lastBlackDelta = 0f
                    historyNotice = context.getString(R.string.game_match_restored)
                } else historyNotice = context.getString(R.string.game_could_not_restore_current_match_was_not_replaced)
                pendingRestore = null
            }) { Text(context.getString(R.string.game_replace_match)) } },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text(context.getString(R.string.game_cancel)) } })
    }
    if (focusView) ToolFocusView(context.getString(R.string.game_chess_scoreboard_86), onExit = { focusView = false }) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(whiteName.ifBlank { context.getString(R.string.game_white) }, style = MaterialTheme.typography.titleLarge)
            Text(whiteScore.toString(), style = MaterialTheme.typography.displayMedium)
            Text(blackName.ifBlank { context.getString(R.string.game_black) }, style = MaterialTheme.typography.titleLarge)
            Text(blackScore.toString(), style = MaterialTheme.typography.displayMedium)
            Text(context.getString(R.string.game_games_1_s, gamesPlayed))
        }
    }

}

@Composable
private fun ScoreColumn(name: String, score: Float, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = if (score == score.toInt().toFloat()) score.toInt()
                .toString() else score.toString(),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
