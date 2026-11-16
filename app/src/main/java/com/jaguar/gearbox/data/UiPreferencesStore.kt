package com.jaguar.gearbox.data

import android.content.Context
import android.util.AtomicFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File
import java.lang.ref.WeakReference

/** Presentation preferences are independent of tool history and never included in cloud backup. */
data class UiPreferences(val reduceMotion: Boolean = false, val showActiveClockShortcut: Boolean = true,
    val defaultUnitSystem: DefaultUnitSystem = DefaultUnitSystem.METRIC)

class UiPreferencesStore private constructor(private val file: File) {
    constructor(context: Context) : this(File(context.applicationContext.noBackupFilesDir, "ui_preferences_v1.json"))
    private val key = runCatching { file.canonicalPath }.getOrDefault(file.absolutePath)
    private val shared: MutableStateFlow<UiPreferences> = synchronized(lock) {
        states.entries.removeAll { it.value.get() == null }
        states[key]?.get() ?: MutableStateFlow(read() ?: UiPreferences()).also { states[key] = WeakReference(it) }
    }
    val preferences: StateFlow<UiPreferences> = shared.asStateFlow()
    val reduceMotion: Boolean get() = preferences.value.reduceMotion
    val showActiveClockShortcut: Boolean get() = preferences.value.showActiveClockShortcut

    fun setDefaultUnitSystem(value: DefaultUnitSystem): Boolean = update { it.copy(defaultUnitSystem = value) }

    fun setReduceMotion(value: Boolean): Boolean = update { it.copy(reduceMotion = value) }
    fun setShowActiveClockShortcut(value: Boolean): Boolean = update { it.copy(showActiveClockShortcut = value) }

    /** Serialize read-modify-write across instances, preserving unrelated preferences. Call on IO. */
    private fun update(transform: (UiPreferences) -> UiPreferences): Boolean = synchronized(lock) {
        val current = read() ?: shared.value
        val next = transform(current)
        if (next == current || write(next)) {
            shared.value = next
            true
        } else false
    }

    private fun read(): UiPreferences? = runCatching {
        AtomicFile(file).openRead().use { stream ->
            val buffer = ByteArray(4097)
            var size = 0
            while (size < buffer.size) {
                val count = stream.read(buffer, size, buffer.size - size)
                if (count < 0) break
                size += count
            }
            if (size > 4096) return@use null
            val json = JSONObject(String(buffer, 0, size, Charsets.UTF_8))
            UiPreferences(
                reduceMotion = json.opt("reduceMotion") as? Boolean ?: false,
                showActiveClockShortcut = json.opt("showActiveClockShortcut") as? Boolean ?: true,
                defaultUnitSystem = DefaultUnitSystem.fromStored(json.opt("defaultUnitSystem") as? String),
            )
        }
    }.getOrNull()

    private fun write(value: UiPreferences): Boolean {
        if (file.parentFile?.let { it.isDirectory || it.mkdirs() } != true) return false
        val atomic = AtomicFile(file)
        var stream: java.io.FileOutputStream? = null
        return try {
            stream = atomic.startWrite()
            stream.write(JSONObject().put("reduceMotion", value.reduceMotion)
                .put("showActiveClockShortcut", value.showActiveClockShortcut)
                .put("defaultUnitSystem", value.defaultUnitSystem.name).toString().toByteArray(Charsets.UTF_8))
            atomic.finishWrite(stream)
            true
        } catch (_: Exception) {
            stream?.let { runCatching { atomic.failWrite(it) } }
            false
        }
    }

    companion object {
        private val lock = Any()
        private val states = mutableMapOf<String, WeakReference<MutableStateFlow<UiPreferences>>>()
        /** Isolated test storage; production always uses the no-backup directory. */
        fun inDirectory(directory: File) = UiPreferencesStore(File(directory, "ui_preferences_v1.json"))
    }
}
