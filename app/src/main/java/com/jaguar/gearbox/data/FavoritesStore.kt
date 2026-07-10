package com.jaguar.gearbox.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * Small persistent store for favourite tools, backed by [android.content.SharedPreferences].
 *
 * The original Java app shipped an empty "Favourite Tools" fragment; this makes the feature
 * actually work. Favourites are keyed by a tool's [Tool.route] so they survive if display names
 * change.
 */
class FavoritesStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Observable list of favourited routes; recomposition happens automatically on change. */
    val favorites: SnapshotStateList<String> =
        mutableStateListOf<String>().also { it.addAll(readFromPrefs()) }

    fun isFavorite(route: String): Boolean = favorites.contains(route)

    fun toggle(route: String) {
        if (!favorites.remove(route)) {
            favorites.add(route)
        }
        persist()
    }

    private fun readFromPrefs(): Set<String> =
        prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()

    private fun persist() {
        prefs.edit().putStringSet(KEY_FAVORITES, favorites.toSet()).apply()
    }

    private companion object {
        const val PREFS_NAME = "gearbox_prefs"
        const val KEY_FAVORITES = "favorite_routes"
    }
}
