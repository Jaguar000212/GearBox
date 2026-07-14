package com.jaguar.gearbox.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.content.edit

/**
 * Tracks the most-recently-opened tools (most recent first), so the home screen can surface a
 * quick-access row instead of making users search or scroll back to the same 2-3 tools they
 * reopen every day.
 */
class RecentToolsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Observable, most-recent-first list of routes; recomposition happens automatically on change. */
    val recent: SnapshotStateList<String> =
        mutableStateListOf<String>().also { it.addAll(readFromPrefs()) }

    fun recordOpened(route: String) {
        recent.remove(route)
        recent.add(0, route)
        while (recent.size > MAX_RECENTS) recent.removeAt(recent.lastIndex)
        persist()
    }

    private fun readFromPrefs(): List<String> =
        prefs.getString(KEY_RECENTS, "")?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()

    private fun persist() {
        prefs.edit { putString(KEY_RECENTS, recent.joinToString(",")) }
    }

    private companion object {
        const val PREFS_NAME = "gearbox_prefs"
        const val KEY_RECENTS = "recent_routes"
        const val MAX_RECENTS = 5
    }
}
