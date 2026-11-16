package com.jaguar.gearbox.data

import android.content.SharedPreferences
import androidx.core.content.edit

/** Writes only its own key; existing app preferences need no migration. */
class LaunchScreenStore(private val prefs: SharedPreferences) {
    fun load(): LaunchScreen = LaunchScreen.fromStored(
        runCatching { prefs.getString(KEY, null) }.getOrNull(),
    )

    fun save(screen: LaunchScreen) = prefs.edit { putString(KEY, screen.name) }

    private companion object { const val KEY = "settings.launch_screen" }
}
