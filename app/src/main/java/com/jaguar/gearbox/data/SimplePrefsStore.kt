package com.jaguar.gearbox.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Generic key-value persistence for tools whose state is simple enough not to warrant a
 * dedicated store class (unlike list-shaped stores such as [ScoreboardStore]). Backed by the
 * same shared-preferences file as [FavoritesStore]/[ScoreboardStore], so callers must prefix
 * their keys with the tool name to avoid collisions.
 *
 * `rememberSaveable` alone only survives rotation, not back-navigation (`popBackStack` destroys
 * the composable) or the process being killed from Recents. This closes that gap for tools whose
 * state a user would reasonably expect to still be there next time they open the tool.
 */
class SimplePrefsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getInt(key: String, default: Int): Int = prefs.getInt(key, default)
    fun getLong(key: String, default: Long): Long = prefs.getLong(key, default)
    fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)
    fun getString(key: String, default: String): String = prefs.getString(key, default) ?: default

    fun getIntList(key: String): List<Int> =
        getString(key, "").split(",").mapNotNull { it.toIntOrNull() }

    fun getLongList(key: String): List<Long> =
        getString(key, "").split(",").mapNotNull { it.toLongOrNull() }

    fun putInt(key: String, value: Int) = prefs.edit { putInt(key, value) }
    fun putBoolean(key: String, value: Boolean) = prefs.edit { putBoolean(key, value) }
    fun putString(key: String, value: String) = prefs.edit { putString(key, value) }

    /** Batches several writes into a single commit; prefer this over multiple single-key puts. */
    fun edit(block: SharedPreferences.Editor.() -> Unit) = prefs.edit(action = block)

    private companion object {
        const val PREFS_NAME = "gearbox_prefs"
    }
}
