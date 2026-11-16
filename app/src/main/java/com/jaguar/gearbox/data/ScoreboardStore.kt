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
class ScoreboardStore(context: Context, private val key: String,
    private val sessionPolicy: GameSessionPolicy = GameSessionPolicy(context),
) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var unreadableV2: String? = null

    val players: SnapshotStateList<ScorePlayer> =
        mutableStateListOf<ScorePlayer>().also { players ->
            players.addAll(sessionPolicy.withPolicy { if (it) readFromPrefs() else emptyList() })
        }

    val hasUnreadableSavedData: Boolean
        get() = hasRetainedLegacyData || unreadableV2 != null

    val hasRetainedLegacyData: Boolean get() = sessionPolicy.isEnabled() && prefs.contains(key)

    val isReadOnly: Boolean get() = unreadableV2 != null

    fun add(player: ScorePlayer) {
        if (isReadOnly) return
        players.add(player)
        persist()
    }

    fun update(index: Int, player: ScorePlayer) {
        if (isReadOnly) return
        players[index] = player
        persist()
    }

    /** Adjust the latest state, not the score captured by the last composition. */
    fun adjustScore(index: Int, delta: Int): Boolean {
        if (isReadOnly) return false
        val current = players.getOrNull(index) ?: return false
        val next = current.score.toLong() + delta
        if (next !in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) return false
        players[index] = current.copy(score = next.toInt())
        persist()
        return true
    }

    fun removeAt(index: Int) {
        if (isReadOnly) return
        if (index !in players.indices) return
        players.removeAt(index)
        persist()
    }

    fun resetScores() {
        if (isReadOnly) return
        for (i in players.indices) {
            players[i] = players[i].copy(score = 0)
        }
        persist()
    }

    fun clear() {
        if (isReadOnly) return
        players.clear()
        persist()
    }

    /** A confirmed, fully validated replacement, preserving retained legacy recovery data. */
    fun restoreSnapshot(values: Map<String, String>): Boolean {
        if (isReadOnly) return false
        val restored = ScoreSnapshots.readScoreboard(values) ?: return false
        val saved = sessionPolicy.withPolicy { enabled ->
            !enabled || prefs.edit().putString("${key}_v2", encodeV2(restored)).commit()
        }
        if (!saved) return false
        androidx.compose.runtime.snapshots.Snapshot.withMutableSnapshot {
            players.clear()
            players.addAll(restored)
        }
        return true
    }

    private fun decodeV2(encoded: String): List<ScorePlayer> {
        val array = JSONArray(encoded)
        return List(array.length()) { i ->
            val item = array.getJSONObject(i)
            ScorePlayer(item.getString("name"), item.get("score").toString().toInt())
        }
    }

    private fun encodeV2(values: List<ScorePlayer>): String {
        val array = JSONArray()
        values.forEach { array.put(JSONObject().put("name", it.name).put("score", it.score)) }
        return array.toString()
    }

    private fun decodeLegacyRow(entry: String): ScorePlayer? {
        val parts = entry.split(FIELD_SEPARATOR)
        val score = parts.getOrNull(1)?.toIntOrNull()
        return if (parts.size != 2 || score == null) null else ScorePlayer(parts[0], score)
    }

    /** Only remove legacy data when every row has an exact verified v2 equivalent. */
    private fun retireEquivalentLegacy(values: List<ScorePlayer>) {
        val raw = prefs.getString(key, null) ?: return
        val rows = if (raw.isEmpty()) emptyList() else raw.split(ENTRY_SEPARATOR)
        val decoded = rows.map { decodeLegacyRow(it) }
        if (decoded.any { it == null } || decoded.filterNotNull() != values) return
        val verified = prefs.getString("${key}_v2", null)
            ?.let { runCatching { decodeV2(it) }.getOrNull() }
        if (verified == values && prefs.getString(key, null) == raw) {
            prefs.edit().remove(key).commit()
        }
    }

    private fun readFromPrefs(): List<ScorePlayer> {
        prefs.getString("${key}_v2", null)?.let { encoded ->
            val decoded = runCatching { decodeV2(encoded) }.getOrNull()
            if (decoded != null) {
                retireEquivalentLegacy(decoded)
                return decoded
            }
            unreadableV2 = encoded
        }
        val raw = prefs.getString(key, null) ?: return emptyList()
        val decoded = raw.split(ENTRY_SEPARATOR).filter { it.isNotEmpty() }
            .mapNotNull(::decodeLegacyRow)
        if (!isReadOnly) {
            // Migrate before the first user edit, then read back and compare before
            // retiring a fully readable legacy value. Ambiguous rows stay untouched.
            if (prefs.edit().putString("${key}_v2", encodeV2(decoded)).commit()) {
                retireEquivalentLegacy(decoded)
            }
        }
        return decoded
    }

    private fun persist() {
        if (isReadOnly) return
        sessionPolicy.runIfEnabled {
            prefs.edit { putString("${key}_v2", encodeV2(players.toList())) }
        }
    }

    private companion object {
        const val PREFS_NAME = "gearbox_prefs"

        // Legacy format only; unreadable or non-equivalent raw values are retained.
        const val FIELD_SEPARATOR = ""
        const val ENTRY_SEPARATOR = ""
    }
}
