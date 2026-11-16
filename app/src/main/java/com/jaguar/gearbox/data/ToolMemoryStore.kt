package com.jaguar.gearbox.data

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * User-controlled tool memory, excluded from Android cloud backup and device transfer by storing
 * it under noBackupFilesDir. No API accepts password-generator data. All writes are atomic and
 * serialized across instances; corrupt files fail closed without crashing a tool.
 *
 * Disabling history pauses recording; existing history stays accessible. Disabling remembered
 * modes/results stops loading and saving them without silently deleting them. Explicit clear
 * actions delete records. Lowering the history limit trims stored tool histories.
 */
class ToolMemoryStore private constructor(private val directory: File) {
    constructor(context: Context) : this(File(context.applicationContext.noBackupFilesDir, "tool_memory_v1"))

    fun settings(): ToolMemorySettings = synchronized(lock) { readSettings() }

    fun saveSettings(settings: ToolMemorySettings): Boolean = synchronized(lock) {
        val value = settings.normalized()
        val previousLimit = readSettings().historyLimit
        val saved = write("settings.json", JSONObject().put("version", 1)
            .put("historyEnabled", value.historyEnabled).put("historyLimit", value.historyLimit)
            .put("rememberModes", value.rememberModes).put("rememberResults", value.rememberResults))
        var trimmed = true
        if (saved && value.historyLimit < previousLimit) {
            directory.listFiles()?.filter { it.name.endsWith(".history.json") }?.forEach { file ->
                val entries = readEntries(file.name).take(value.historyLimit)
                if (!writeEntries(file.name, entries)) trimmed = false
            }
        }
        saved && trimmed
    }

    /** History remains readable when recording is paused, so users can export or delete it. */
    fun history(route: String): List<ToolHistoryEntry> = synchronized(lock) {
        if (!ToolMemoryPolicy.permitsRoute(route)) emptyList()
        else readEntries(fileName(route, "history")).take(readSettings().historyLimit)
    }

    /** Capture before debouncing; clear/delete invalidates in-flight recordings in this process. */
    fun historyGeneration(route: String): Long = synchronized(lock) {
        generations.getOrPut(generationKey(route, "history")) { 0L }
    }

    fun record(route: String, inputs: Map<String, String>, output: String,
        expectedGeneration: Long? = null): Boolean = synchronized(lock) {
        if (expectedGeneration != null && expectedGeneration != historyGeneration(route)) return@synchronized false
        val settings = readSettings()
        if (!settings.historyEnabled || !ToolMemoryPolicy.permitsRoute(route) ||
            !ToolMemoryPolicy.validRecord(inputs, output)) return@synchronized false
        val name = fileName(route, "history")
        val old = readEntries(name)
        // Recomposition or repeated callbacks for the same result must not flood history.
        if (old.firstOrNull()?.let { it.inputs == inputs && it.output == output } == true) return@synchronized true
        val entry = ToolHistoryEntry(UUID.randomUUID().toString(), System.currentTimeMillis(), inputs.toMap(), output)
        writeEntries(name, (listOf(entry) + old).take(settings.historyLimit))
    }

    fun remove(route: String, id: String): Boolean = synchronized(lock) {
        if (!ToolMemoryPolicy.permitsRoute(route)) false
        else {
            val success = writeEntries(fileName(route, "history"), history(route).filterNot { it.id == id })
            if (success) bumpGeneration(route, "history")
            success
        }
    }

    fun clearHistory(route: String? = null): Boolean = synchronized(lock) {
        if (route != null && !ToolMemoryPolicy.permitsRoute(route)) return@synchronized false
        val success = if (route != null) delete(fileName(route, "history")) else clearSuffix(".history.json")
        if (success) {
            if (route != null) bumpGeneration(route, "history")
            else bumpAllGenerations("history")
        }
        success
    }

    /** Modes contain configuration only, not user text or generated secrets. */
    fun modeGeneration(route: String): Long = synchronized(lock) {
        generations.getOrPut(generationKey(route, "mode")) { 0L }
    }
    fun resultGeneration(route: String): Long = synchronized(lock) {
        generations.getOrPut(generationKey(route, "result")) { 0L }
    }

    fun saveMode(route: String, values: Map<String, String>, expectedGeneration: Long? = null): Boolean = synchronized(lock) {
        if (expectedGeneration != null && expectedGeneration != modeGeneration(route)) return@synchronized false
        if (!readSettings().rememberModes || !ToolMemoryPolicy.permitsRoute(route) ||
            !ToolMemoryPolicy.validInputs(values)) false
        else write(fileName(route, "mode"), JSONObject().put("version", 1).put("values", JSONObject(values)))
    }

    fun loadMode(route: String): Map<String, String> = synchronized(lock) {
        if (!readSettings().rememberModes || !ToolMemoryPolicy.permitsRoute(route)) emptyMap()
        else parseInputs(read(fileName(route, "mode"))?.optJSONObject("values")) ?: emptyMap()
    }

    fun saveResult(route: String, inputs: Map<String, String>, output: String,
        expectedGeneration: Long? = null): Boolean = synchronized(lock) {
        if (expectedGeneration != null && expectedGeneration != resultGeneration(route)) return@synchronized false
        if (!readSettings().rememberResults || !ToolMemoryPolicy.permitsRoute(route) ||
            !ToolMemoryPolicy.validRecord(inputs, output)) false
        else writeEntries(fileName(route, "result"), listOf(ToolHistoryEntry(
            UUID.randomUUID().toString(), System.currentTimeMillis(), inputs.toMap(), output)))
    }

    fun loadResult(route: String): ToolHistoryEntry? = synchronized(lock) {
        if (!readSettings().rememberResults || !ToolMemoryPolicy.permitsRoute(route)) null
        else readEntries(fileName(route, "result")).firstOrNull()
    }

    fun clearRememberedModes(): Boolean = synchronized(lock) {
        clearSuffix(".mode.json").also { if (it) bumpAllGenerations("mode") }
    }
    fun clearRememberedResults(): Boolean = synchronized(lock) {
        clearSuffix(".result.json").also { if (it) bumpAllGenerations("result") }
    }

    /** Explicit named configuration saves are independent of automatic remembering settings. */
    fun presets(): List<SavedToolItem> = synchronized(lock) { readSaved("presets.json") }
    fun savedResults(): List<SavedToolItem> = synchronized(lock) { readSaved("workbench.json") }

    fun savePreset(name: String, route: String, configuration: Map<String, String>): Boolean = synchronized(lock) {
        saveNamed("presets.json", name, route, configuration, "")
    }

    fun saveToWorkbench(name: String, route: String, inputs: Map<String, String>, output: String): Boolean = synchronized(lock) {
        saveNamed("workbench.json", name, route, inputs, output)
    }

    fun removePreset(id: String): Boolean = synchronized(lock) {
        writeSaved("presets.json", presets().filterNot { it.id == id })
    }
    fun removeSavedResult(id: String): Boolean = synchronized(lock) {
        writeSaved("workbench.json", savedResults().filterNot { it.id == id })
    }
    fun clearPresets(): Boolean = synchronized(lock) { delete("presets.json") }
    fun clearSavedResults(): Boolean = synchronized(lock) { delete("workbench.json") }

    private fun saveNamed(file: String, name: String, route: String, inputs: Map<String, String>, output: String): Boolean {
        val label = name.trim()
        if (label.isBlank() || label.length > 80 || !ToolMemoryPolicy.permitsRoute(route) ||
            !ToolMemoryPolicy.validRecord(inputs, output)) return false
        val existing = readSaved(file)
        // Unlike automatic history, explicit saves never silently evict an older saved item.
        if (existing.size >= MAX_SAVED_ITEMS) return false
        return writeSaved(file, listOf(SavedToolItem(UUID.randomUUID().toString(), label, route,
            System.currentTimeMillis(), inputs.toMap(), output)) + existing)
    }

    private fun readSaved(name: String): List<SavedToolItem> {
        val items = read(name)?.optJSONArray("items") ?: return emptyList()
        return (0 until minOf(items.length(), MAX_SAVED_ITEMS)).mapNotNull { index ->
            val item = items.optJSONObject(index) ?: return@mapNotNull null
            val id = item.optString("id").takeIf { it.isNotBlank() && it.length <= 64 } ?: return@mapNotNull null
            val label = item.optString("name").takeIf { it.isNotBlank() && it.length <= 80 } ?: return@mapNotNull null
            val route = item.optString("route").takeIf(ToolMemoryPolicy::permitsRoute) ?: return@mapNotNull null
            val time = item.optLong("timestampMillis", -1).takeIf { it >= 0 } ?: return@mapNotNull null
            val inputs = parseInputs(item.optJSONObject("inputs")) ?: return@mapNotNull null
            val output = item.opt("output") as? String ?: return@mapNotNull null
            if (!ToolMemoryPolicy.validRecord(inputs, output)) return@mapNotNull null
            SavedToolItem(id, label, route, time, inputs, output)
        }
    }

    private fun writeSaved(name: String, values: List<SavedToolItem>): Boolean {
        val items = JSONArray()
        values.forEach { item -> items.put(JSONObject().put("id", item.id).put("name", item.name)
            .put("route", item.route).put("timestampMillis", item.timestampMillis)
            .put("inputs", JSONObject(item.inputs)).put("output", item.output)) }
        return write(name, JSONObject().put("version", 1).put("items", items))
    }

    private fun readSettings(): ToolMemorySettings {
        val json = read("settings.json") ?: return ToolMemorySettings()
        return ToolMemorySettings(json.optBoolean("historyEnabled", false), json.optInt("historyLimit", 20),
            json.optBoolean("rememberModes", false), json.optBoolean("rememberResults", false)).normalized()
    }

    private fun readEntries(name: String): List<ToolHistoryEntry> {
        val array = read(name)?.optJSONArray("entries") ?: return emptyList()
        return (0 until minOf(array.length(), ToolMemorySettings.MAX_HISTORY)).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val id = item.optString("id").takeIf { it.isNotBlank() && it.length <= 64 } ?: return@mapNotNull null
            val timestamp = item.optLong("timestampMillis", -1).takeIf { it >= 0 } ?: return@mapNotNull null
            val inputs = parseInputs(item.optJSONObject("inputs")) ?: return@mapNotNull null
            val output = item.opt("output") as? String ?: return@mapNotNull null
            if (!ToolMemoryPolicy.validRecord(inputs, output)) return@mapNotNull null
            ToolHistoryEntry(id, timestamp, inputs, output)
        }
    }

    private fun parseInputs(json: JSONObject?): Map<String, String>? {
        if (json == null || json.length() > ToolMemoryPolicy.MAX_FIELDS) return null
        val values = mutableMapOf<String, String>()
        json.keys().forEach { key -> values[key] = json.opt(key) as? String ?: return null }
        return values.takeIf(ToolMemoryPolicy::validInputs)
    }

    private fun writeEntries(name: String, entries: List<ToolHistoryEntry>): Boolean {
        val array = JSONArray()
        entries.forEach { entry -> array.put(JSONObject().put("id", entry.id)
            .put("timestampMillis", entry.timestampMillis).put("inputs", JSONObject(entry.inputs)).put("output", entry.output)) }
        return write(name, JSONObject().put("version", 1).put("entries", array))
    }

    private fun read(name: String): JSONObject? = runCatching {
        val file = AtomicFile(File(directory, name))
        file.openRead().use { input ->
            val buffer = java.io.ByteArrayOutputStream()
            val chunk = ByteArray(8192)
            while (true) {
                val count = input.read(chunk)
                if (count < 0) break
                if (buffer.size() + count > ToolMemoryPolicy.MAX_FILE_BYTES) return null
                buffer.write(chunk, 0, count)
            }
            val bytes = buffer.toByteArray()
            JSONObject(bytes.toString(Charsets.UTF_8)).takeIf { it.optInt("version") == 1 }
        }
    }.getOrNull()

    private fun write(name: String, json: JSONObject): Boolean {
        val bytes = json.toString().toByteArray(Charsets.UTF_8)
        if (bytes.size > ToolMemoryPolicy.MAX_FILE_BYTES || (!directory.isDirectory && !directory.mkdirs())) return false
        val file = AtomicFile(File(directory, name))
        val output = runCatching { file.startWrite() }.getOrNull() ?: return false
        return try { output.write(bytes); file.finishWrite(output); true }
        catch (_: Exception) { file.failWrite(output); false }
    }

    private fun clearSuffix(suffix: String): Boolean = directory.listFiles()
        ?.filter { it.name.endsWith(suffix) || it.name.endsWith("$suffix.bak") || it.name.endsWith("$suffix.new") }
        ?.map { it.name.removeSuffix(".bak").removeSuffix(".new") }?.distinct()
        ?.map { delete(it) }?.all { it } ?: true

    private fun delete(name: String): Boolean {
        val file = File(directory, name)
        AtomicFile(file).delete()
        return !file.exists() && !File(directory, "$name.bak").exists() && !File(directory, "$name.new").exists()
    }

    private fun generationKey(route: String, kind: String) = directory.absolutePath + "|" + kind + "|" + route
    private fun bumpGeneration(route: String, kind: String) {
        val key = generationKey(route, kind)
        generations[key] = (generations[key] ?: 0L) + 1L
    }

    private fun bumpAllGenerations(kind: String) {
        generations.keys.filter { it.startsWith(directory.absolutePath + "|" + kind + "|") }.toList()
            .forEach { key -> generations[key] = (generations[key] ?: 0L) + 1L }
    }

    private fun fileName(route: String, kind: String) = "${route.removePrefix("tool/")}.$kind.json"

    companion object {
        const val MAX_SAVED_ITEMS = 100
        private val lock = Any()
        private val generations = mutableMapOf<String, Long>()
        /** Isolated storage for instrumentation tests; never points at production user data. */
        fun inDirectory(directory: File): ToolMemoryStore = ToolMemoryStore(directory)
    }
}
