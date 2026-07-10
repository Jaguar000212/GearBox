package com.jaguar.gearbox.data

import android.content.Context
import androidx.core.content.edit

/** Snapshot of an in-progress chess match tracked by [ChessMatchStore]. */
data class ChessMatch(
    val whiteName: String,
    val blackName: String,
    val whiteScore: Float,
    val blackScore: Float,
    val gamesPlayed: Int,
)

/**
 * Persists a [ChessMatch] to [android.content.SharedPreferences] so the match survives fully
 * closing and reopening the app, not just a configuration change.
 */
class ChessMatchStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): ChessMatch = ChessMatch(
        whiteName = prefs.getString(KEY_WHITE_NAME, null) ?: "White",
        blackName = prefs.getString(KEY_BLACK_NAME, null) ?: "Black",
        whiteScore = prefs.getFloat(KEY_WHITE_SCORE, 0f),
        blackScore = prefs.getFloat(KEY_BLACK_SCORE, 0f),
        gamesPlayed = prefs.getInt(KEY_GAMES_PLAYED, 0),
    )

    fun save(match: ChessMatch) {
        prefs.edit {
            putString(KEY_WHITE_NAME, match.whiteName)
                .putString(KEY_BLACK_NAME, match.blackName)
                .putFloat(KEY_WHITE_SCORE, match.whiteScore)
                .putFloat(KEY_BLACK_SCORE, match.blackScore)
                .putInt(KEY_GAMES_PLAYED, match.gamesPlayed)
        }
    }

    private companion object {
        const val PREFS_NAME = "gearbox_prefs"
        const val KEY_WHITE_NAME = "chess_white_name"
        const val KEY_BLACK_NAME = "chess_black_name"
        const val KEY_WHITE_SCORE = "chess_white_score"
        const val KEY_BLACK_SCORE = "chess_black_score"
        const val KEY_GAMES_PLAYED = "chess_games_played"
    }
}
