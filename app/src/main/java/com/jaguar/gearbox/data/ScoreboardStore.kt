package com.jaguar.gearbox.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

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

    /** The untouched legacy data remains available if a row could not be decoded. */
    val hasUnrecoverableLegacyRows: Boolean = prefs.getString(key, null)?.split(ENTRY_SEPARATOR)
        ?.filter { it.isNotEmpty() }?.any {
            val parts = it.split(FIELD_SEPARATOR)
            parts.size != 2 || parts.getOrNull(1)?.toIntOrNull() == null
        } ?: false

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

    /** Adjust the latest state, not the score captured by the last composition. */
    fun adjustScore(index: Int, delta: Int): Boolean {
        val current = players.getOrNull(index) ?: return false
        val next = current.score.toLong() + delta
        if (next !in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) return false
        players[index] = current.copy(score = next.toInt())
        persist()
        return true
    }

    fun removeAt(index: Int) {
        if (index !in players.indices) return
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
        prefs.getString("${key}_v2", null)?.let { encoded ->
            runCatching {
                val array = JSONArray(encoded)
                return List(array.length()) { i ->
                    val item = array.getJSONObject(i)
                    ScorePlayer(item.getString("name"), item.getInt("score"))
                }
            }
        }
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
        val array = JSONArray()
        players.forEach { player ->
            array.put(JSONObject().put("name", player.name).put("score", player.score))
        }
        // Never overwrite the legacy raw value: malformed/ambiguous rows cannot be
        // reconstructed reliably, so retain the original for recovery after migration.
        prefs.edit { putString("${key}_v2", array.toString()) }
    }

    private companion object {
        const val PREFS_NAME = "gearbox_prefs"

        // Legacy format only; new writes use escaped JSON and retain the original raw data.
        const val FIELD_SEPARATOR = ""
        const val ENTRY_SEPARATOR = ""
    }
}
