package com.jaguar.gearbox.data

import android.content.Context
import com.jaguar.gearbox.R
import com.jaguar.gearbox.logic.*
import com.jaguar.gearbox.ui.tools.RpsMove
import com.jaguar.gearbox.ui.tools.RpsOutcome

internal fun RpsMove.localizedLabel(context: Context): String = context.getString(when (this) {
    RpsMove.ROCK -> R.string.catalog_rps_rock
    RpsMove.PAPER -> R.string.catalog_rps_paper
    RpsMove.SCISSORS -> R.string.catalog_rps_scissors
})
internal fun RpsOutcome.localizedMessage(context: Context): String = context.getString(when (this) {
    RpsOutcome.WIN -> R.string.catalog_rps_win
    RpsOutcome.LOSS -> R.string.catalog_rps_loss
    RpsOutcome.DRAW -> R.string.catalog_rps_draw
})
fun TambolaSnapshot.localizedSummary(context: Context): String = context.getString(
    R.string.catalog_tambola_summary, calls.size, last, calls.joinToString(", "))
fun TicTacToeSnapshot.localizedSummary(context: Context): String {
    val cells = board.map { it.ifEmpty { null } }
    val winner = winningLine(cells)?.let { cells[it[0]] }
    val status = when {
        winner != null -> context.getString(R.string.catalog_ttt_win, winner)
        isBoardFull(cells) -> context.getString(R.string.catalog_rps_draw)
        else -> context.getString(R.string.catalog_ttt_turn, nextPlayer)
    }
    val grid = board.chunked(3).joinToString("\n") { row -> row.joinToString(" | ") { it.ifEmpty { "·" } } }
    return context.getString(R.string.catalog_ttt_summary, status, grid)
}
/** Read-only and locale-aware; never changes active clock state. */
fun localizedHomeClockSummary(context: Context, state: ClockState): String? {
    val statuses = buildList {
        when {
            state.timerRunning -> add(context.getString(R.string.catalog_timer_running))
            state.timerFinished -> add(context.getString(R.string.catalog_timer_finished))
            state.timerRemainingMillis > 0 -> add(context.getString(R.string.catalog_timer_paused))
        }
        when {
            state.stopwatchRunning -> add(context.getString(R.string.catalog_stopwatch_running))
            state.stopwatchElapsedMillis > 0 -> add(context.getString(R.string.catalog_stopwatch_paused))
        }
    }
    return statuses.takeIf { it.isNotEmpty() }?.joinToString(context.getString(R.string.catalog_status_separator))
}
