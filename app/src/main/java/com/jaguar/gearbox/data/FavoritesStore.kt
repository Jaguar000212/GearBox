package com.jaguar.gearbox.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.content.edit

/**
 * Small persistent store for favourite tools, backed by [android.content.SharedPreferences].
 *
 * The original Java app shipped an empty "Favourite Tools" fragment; this makes the feature
 * actually work. Favourites are keyed by a tool's [Tool.route] so they survive if display names
 * change, and stored as an ordered, delimited string (rather than a [Set]) so the user's manual
 * reordering of their favourites list actually persists.
 */
class FavoritesStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Observable list of favourited routes, in display order; recomposes automatically on change. */
    val favorites: SnapshotStateList<String> =
        mutableStateListOf<String>().also { it.addAll(readFromPrefs()) }

    fun isFavorite(route: String): Boolean = favorites.contains(route)

    fun toggle(route: String) {
        if (!favorites.remove(route)) {
            favorites.add(route)
        }
        persist()
    }

    /** Moves the favourite at [from] to [to], shifting the rest, then persists the new order. */
    fun move(from: Int, to: Int) {
        if (from == to || from !in favorites.indices || to !in favorites.indices) return
        favorites.add(to, favorites.removeAt(from))
        persist()
    }

    private fun readFromPrefs(): List<String> {
        val ordered = runCatching { prefs.getString(KEY_FAVORITES, null) }.getOrNull()
        if (ordered != null) return ordered.split(",").filter { it.isNotEmpty() }

        // Older installs stored this key as an unordered StringSet; migrate it once to the new
        // ordered format so reordering has something stable to persist into.
        val legacy = runCatching { prefs.getStringSet(KEY_FAVORITES, null) }.getOrNull()
        if (legacy != null) {
            prefs.edit { putString(KEY_FAVORITES, legacy.joinToString(",")) }
            return legacy.toList()
        }

        return emptyList()
    }

    private fun persist() {
        prefs.edit { putString(KEY_FAVORITES, favorites.joinToString(",")) }
    }

    private companion object {
        const val PREFS_NAME = "gearbox_prefs"
        const val KEY_FAVORITES = "favorite_routes"
    }
}
