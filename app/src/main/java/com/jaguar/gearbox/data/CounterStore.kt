package com.jaguar.gearbox.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.jaguar.gearbox.logic.nextCounterValue

/** One atomic read/modify/write path shared by the screen and every widget instance. */
class CounterStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("gearbox_prefs", Context.MODE_PRIVATE)
    val count: Int get() = prefs.getInt(KEY_COUNT, 0)
    val limitReached: Boolean get() = prefs.getBoolean(KEY_LIMIT, false)

    fun adjust(delta: Int) = synchronized(lock) {
        val next = nextCounterValue(count, delta)
        prefs.edit {
            if (next != null) putInt(KEY_COUNT, next)
            putBoolean(KEY_LIMIT, next == null)
        }
    }

    fun reset() = synchronized(lock) {
        prefs.edit { putInt(KEY_COUNT, 0); putBoolean(KEY_LIMIT, false) }
    }

    /** Caller retains the returned cleanup callback, keeping the preference listener alive. */
    fun observe(onChange: () -> Unit): () -> Unit {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_COUNT || key == KEY_LIMIT || key == null) onChange()
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        return { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    private companion object {
        const val KEY_COUNT = "counter.count"
        const val KEY_LIMIT = "counter.limit_reached"
        val lock = Any()
    }
}
