package com.jaguar.gearbox.data

import android.content.Context
import android.content.SharedPreferences
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
class FavoritesStore(
    private val prefs: SharedPreferences,
    private val syncShortcuts: (List<String>) -> Unit = {},
) {
    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
        { routes -> updateDynamicShortcuts(context.applicationContext, routes) },
    )

    /** Observable list of favourited routes, in display order; recomposes automatically on change. */
    val favorites: SnapshotStateList<String> =
        mutableStateListOf<String>().also { it.addAll(readFromPrefs()) }

    init {
        // Re-sync on cold start too, e.g. after a reinstall wiped the launcher's own shortcut state.
        syncShortcuts(favorites.toList())
    }

    fun isFavorite(route: String): Boolean = favorites.contains(route)

    fun toggle(route: String) {
        if (!favorites.remove(route)) {
            favorites.add(route)
        }
        persist()
    }

    /** Add without disturbing the order of existing favorites. */
    fun addAll(routes: List<String>) {
        routes.distinct().filterNot { it in favorites }.forEach { favorites.add(it) }
        persist()
    }

    /** Restore an explicitly removed item at its previous position. */
    fun restore(route: String, position: Int) {
        if (route in favorites) return
        favorites.add(position.coerceIn(0, favorites.size), route)
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
        syncShortcuts(favorites.toList())
    }

    private companion object {
        const val PREFS_NAME = "gearbox_prefs"
        const val KEY_FAVORITES = "favorite_routes"
    }
}
