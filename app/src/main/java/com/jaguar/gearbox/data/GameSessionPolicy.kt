package com.jaguar.gearbox.data

import android.content.Context

/** Controls automatic game/participant retention, not clock/counter state or explicit history. */
class GameSessionPolicy(
    context: Context,
    private val memory: ToolMemoryStore = ToolMemoryStore(context),
) {
    private val prefs = context.applicationContext.getSharedPreferences("gearbox_prefs", Context.MODE_PRIVATE)
    fun isEnabled(): Boolean = memory.settings().rememberGameSessions
    internal fun <T> withPolicy(block: (Boolean) -> T): T = memory.withGameSessionPolicy(block)
    internal fun runIfEnabled(block: () -> Unit) {
        withPolicy { enabled -> if (enabled) block(); Unit }
    }

    /** Explicit destructive action. Other keys, current UI state and saved history remain intact. */
    fun clearSavedSessions(): Boolean = withPolicy {
        val editor = prefs.edit()
        SESSION_KEYS.forEach(editor::remove)
        editor.commit()
    }

    companion object {
        private val SESSION_KEYS = listOf(
            "tambola.called", "tambola.last_called", "rps.wins", "rps.losses", "rps.draws",
            "tic_tac_toe.x_wins", "tic_tac_toe.o_wins", "tic_tac_toe.draws",
            "team_picker.names", "scoreboard_players", "scoreboard_players_v2",
            "chess_white_name", "chess_black_name", "chess_white_score", "chess_black_score", "chess_games_played",
        )
    }
}
