package com.jaguar.gearbox.data

import android.content.Context
import android.content.SharedPreferences
import android.util.AtomicFile
import androidx.compose.runtime.getValue
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.lang.ref.WeakReference

/** Recent navigation contains routes only, never tool inputs/results. New data stays out of backup. */
class RecentToolsStore private constructor(
    private val file: File,
    private val legacy: SharedPreferences?,
) {
    constructor(context: Context) : this(
        File(context.applicationContext.noBackupFilesDir, "recent_tools_v1.json"),
        context.applicationContext.getSharedPreferences("gearbox_prefs", Context.MODE_PRIVATE),
    )

    private data class State(val show: Boolean = true, val record: Boolean = true, val routes: List<String> = emptyList())
    private val stateKey = runCatching { file.canonicalPath }.getOrDefault(file.absolutePath)
    private val sharedState: MutableState<State> = synchronized(lock) {
        states.entries.removeAll { it.value.get() == null }
        states[stateKey]?.get() ?: mutableStateOf(load()).also { states[stateKey] = WeakReference(it) }
    }
    private var state by sharedState
    val recent: List<String> get() = state.routes
    val showRecentTools: Boolean get() = state.show
    val recordRecentTools: Boolean get() = state.record

    /** Hiding the row does not stop collection; pausing collection does not erase existing routes. */
    fun setShowRecentTools(value: Boolean): Boolean = update { it.copy(show = value) }
    fun setRecordRecentTools(value: Boolean): Boolean = update { it.copy(record = value) }
    fun clear(): Boolean = update { it.copy(routes = emptyList()) }

    fun recordOpened(route: String): Boolean {
        if (!validRoute.matches(route)) return false
        return update { current ->
            if (!current.record) current
            else current.copy(routes = (listOf(route) + current.routes.filterNot { it == route }).take(MAX_RECENTS))
        }
    }

    private fun update(change: (State) -> State): Boolean = synchronized(lock) {
        // Re-read under the write lock: an older store instance cannot re-enable collection or
        // resurrect routes cleared by another activity/store. Shared state keeps both UIs current.
        val current = readStored() ?: state
        if (current != state) state = current
        val next = change(current)
        if (next == current) return@synchronized true
        if (write(next)) { state = next; true } else false
    }

    private fun readStored(): State? = runCatching {
            AtomicFile(file).openRead().use { stream ->
                val bytes = ByteArray(4097)
                var size = 0
                while (size < bytes.size) {
                    val count = stream.read(bytes, size, bytes.size - size)
                    if (count < 0) break
                    size += count
                }
                if (size > 4096) return@use null
                val json = JSONObject(String(bytes, 0, size, Charsets.UTF_8))
                if (json.optInt("version") != 1) return@use null
                val values = json.optJSONArray("routes") ?: JSONArray()
                State(json.optBoolean("show", true), json.optBoolean("record", true),
                    (0 until minOf(values.length(), MAX_RECENTS)).mapNotNull { index ->
                        (values.opt(index) as? String)?.takeIf(validRoute::matches)
                    }.distinct())
            }
        }.getOrNull()

    private fun load(): State = synchronized(lock) {
        val stored = readStored()
        if (stored != null) {
            // Retry legacy cleanup if a previous process stopped after the atomic migration.
            legacy?.edit()?.remove("recent_routes")?.commit()
            return@synchronized stored
        }
        val routes = runCatching { legacy?.getString("recent_routes", "") }.getOrNull().orEmpty()
            .split(',').filter(validRoute::matches).distinct().take(MAX_RECENTS)
        val migrated = State(routes = routes)
        // Remove only the old navigation key, and only after the replacement is durable.
        if (write(migrated)) legacy?.edit()?.remove("recent_routes")?.commit()
        migrated
    }

    private fun write(value: State): Boolean {
        val parent = file.parentFile ?: return false
        if (!parent.isDirectory && !parent.mkdirs()) return false
        val json = JSONObject().put("version", 1).put("show", value.show).put("record", value.record)
            .put("routes", JSONArray(value.routes)).toString()
        val atomic = AtomicFile(file)
        val stream = runCatching { atomic.startWrite() }.getOrNull() ?: return false
        return try {
            stream.write(json.toByteArray(Charsets.UTF_8)); atomic.finishWrite(stream); true
        } catch (_: Exception) { atomic.failWrite(stream); false }
    }

    companion object {
        const val MAX_RECENTS = 5
        private val validRoute = Regex("tool/[a-z0-9_]{1,64}")
        private val lock = Any()
        private val states = mutableMapOf<String, WeakReference<MutableState<State>>>()
        /** Isolated files and preferences for instrumentation tests. */
        fun inFile(file: File, legacy: SharedPreferences? = null) = RecentToolsStore(file, legacy)
    }
}
