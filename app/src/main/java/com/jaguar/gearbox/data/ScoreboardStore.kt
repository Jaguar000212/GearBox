package com.jaguar.gearbox.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.content.edit

/** A single competitor tracked on a [ScoreboardStore]. */
data class ScorePlayer(val name: String, val score: Int)

/**
 * Persists a list of players/scores to [android.content.SharedPreferences], keyed so multiple
 * scoreboard-shaped tools can each keep their own saved state. Unlike Compose's
 * `rememberSaveable` (which is lost once the process is killed from Recents rather than merely
 * rotated), this survives fully closing and reopening the app — the point of a scoreboard.
 */
class ScoreboardStore(context: Context, private val key: String) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val players: SnapshotStateList<ScorePlayer> =
        mutableStateListOf<ScorePlayer>().also { it.addAll(readFromPrefs()) }

    fun add(player: ScorePlayer) {
        players.add(player)
        persist()
    }

    fun update(index: Int, player: ScorePlayer) {
        players[index] = player
        persist()
    }

    fun removeAt(index: Int) {
        players.removeAt(index)
        persist()
    }

    fun resetScores() {
        for (i in players.indices) {
            players[i] = players[i].copy(score = 0)
        }
        persist()
    }

    fun clear() {
        players.clear()
        persist()
    }

    private fun readFromPrefs(): List<ScorePlayer> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return raw.split(ENTRY_SEPARATOR)
            .filter { it.isNotEmpty() }
            .mapNotNull { entry ->
                val parts = entry.split(FIELD_SEPARATOR)
                val score = parts.getOrNull(1)?.toIntOrNull()
                if (parts.size != 2 || score == null) null else ScorePlayer(parts[0], score)
            }
    }

    private fun persist() {
        val encoded = players.joinToString(ENTRY_SEPARATOR) { "${it.name}$FIELD_SEPARATOR${it.score}" }
        prefs.edit { putString(key, encoded) }
    }

    private companion object {
        const val PREFS_NAME = "gearbox_prefs"
        // Control characters, so a player-entered name can never collide with the delimiters.
        const val FIELD_SEPARATOR = ""
        const val ENTRY_SEPARATOR = ""
    }
}
