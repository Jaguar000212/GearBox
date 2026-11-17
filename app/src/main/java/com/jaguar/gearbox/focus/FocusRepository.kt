package com.jaguar.gearbox.focus

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File
import java.io.IOException

/** Current authoritative session only. No names, history, cloud backup or existing clock keys. */
class FocusRepository(context: Context) {
    private val context = context.applicationContext
    private val file = AtomicFile(File(this.context.noBackupFilesDir, "focus/session-v1.json"))
    private fun boot() = runCatching { Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT) }.getOrDefault(-1)

    fun read(): FocusState = refresh(reschedule = true)

    fun refresh(expectedGeneration: Long? = null, reschedule: Boolean = false): FocusState = synchronized(lock) {
        val saved = load()
        if (expectedGeneration != null && expectedGeneration != saved.generation) return@synchronized saved
        val current = advanceFocus(saved, SystemClock.elapsedRealtime(), boot())
        val claimed = claimFocusBoundary(current)
        val changed = current.status != saved.status || current.phaseIndex != saved.phaseIndex
        val result = claimed ?: current
        if (changed || claimed != null) save(result)
        if (changed || reschedule) FocusAlerts.schedule(context, result)
        if (claimed != null) FocusAlerts.notifyBoundary(context, result)
        result
    }

    fun start(config: FocusConfig, expectedGeneration: Long? = null): FocusState = synchronized(lock) {
        val current = load()
        checkGeneration(current, expectedGeneration)
        commit(startFocus(config, SystemClock.elapsedRealtime(), boot(), current.generation))
    }
    fun pause(expectedGeneration: Long? = null): FocusState = synchronized(lock) {
        val current = load()
        checkGeneration(current, expectedGeneration)
        commit(pauseFocus(current, SystemClock.elapsedRealtime(), boot()))
    }
    fun resume(expectedGeneration: Long? = null): FocusState = synchronized(lock) {
        val current = load()
        checkGeneration(current, expectedGeneration)
        commit(resumeFocus(current, SystemClock.elapsedRealtime(), boot()))
    }
    fun reset(expectedGeneration: Long? = null): FocusState = synchronized(lock) {
        // Explicit Reset is also the recovery path for a corrupt current-session file.
        // A caller recovering corruption must deliberately omit the unavailable generation.
        val old = try { load() } catch (error: IOException) {
            if (expectedGeneration != null) throw error
            FocusState()
        }
        checkGeneration(old, expectedGeneration)
        commit(FocusState(config = old.config, generation = if (old.generation == Long.MAX_VALUE) 1 else old.generation + 1))
    }
    /** Recovery confirmation cannot clear a session repaired/replaced by another Activity. */
    fun resetUnreadable(): FocusState = synchronized(lock) {
        val unreadable = try { load(); false } catch (_: IOException) { true }
        check(unreadable) { "Focus state is readable again; review the current session" }
        commit(FocusState(generation = 1))
    }
    private fun checkGeneration(state: FocusState, expected: Long?) {
        check(expected == null || state.generation == expected) { "Focus session changed; review the current session" }
    }
    private fun commit(state: FocusState): FocusState {
        save(state) // Do not show/schedule a session that failed to persist.
        FocusAlerts.dismiss(context)
        FocusAlerts.schedule(context, state)
        return state
    }

    private fun load(): FocusState {
        if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) return FocusState()
        try {
            val bytes = file.openRead().use { input ->
                val buffer = ByteArray(16385)
                var count = 0
                while (count < buffer.size) {
                    val n = input.read(buffer, count, buffer.size - count)
                    if (n < 0) break
                    count += n
                }
                if (count > 16384) throw IOException("Focus state exceeds limit")
                buffer.copyOf(count)
            }
            return decodeFocusState(JSONObject(bytes.toString(Charsets.UTF_8)))
                ?: throw IOException("Focus state is invalid; reset the session")
        } catch (error: IOException) { throw error }
        catch (error: Exception) { throw IOException("Focus state cannot be read; reset the session", error) }
    }
    private fun save(state: FocusState) {
        if (!file.baseFile.parentFile!!.isDirectory && !file.baseFile.parentFile!!.mkdirs())
            throw IOException("Focus state directory unavailable")
        val output = file.startWrite()
        try { output.write(encodeFocusState(state).toString().toByteArray(Charsets.UTF_8)); file.finishWrite(output) }
        catch (error: Exception) { file.failWrite(output); throw IOException("Focus state could not be saved", error) }
    }
    companion object { private val lock = Any() }
}

internal fun encodeFocusState(s: FocusState): JSONObject = JSONObject().put("version", 1)
    .put("mode", s.config.mode.name).put("work", s.config.workSeconds).put("short", s.config.shortBreakSeconds)
    .put("long", s.config.longBreakSeconds).put("rounds", s.config.rounds).put("every", s.config.longBreakEvery)
    .put("status", s.status.name).put("phase", s.phaseIndex).put("remaining", s.remainingMillis)
    .put("elapsed", s.savedElapsed).put("boot", s.bootCount).put("generation", s.generation).put("alerted", s.alertedPhase)

internal fun decodeFocusState(j: JSONObject): FocusState? = runCatching {
    fun number(key: String): Long {
        val value = j.opt(key)
        require(value is Int || value is Long)
        return (value as Number).toLong()
    }
    fun int(key: String): Int = number(key).also { require(it in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) }.toInt()
    require(j.keys().asSequence().toSet() == setOf("version", "mode", "work", "short", "long", "rounds", "every",
        "status", "phase", "remaining", "elapsed", "boot", "generation", "alerted"))
    require(number("version") == 1L)
    val config = FocusConfig(FocusMode.valueOf(j.getString("mode")), int("work"), int("short"), int("long"), int("rounds"), int("every"))
    require(config.isValid())
    val s = FocusState(config, FocusStatus.valueOf(j.getString("status")), int("phase"), number("remaining"),
        number("elapsed"), int("boot"), number("generation"), int("alerted"))
    require(validFocusState(s))
    s
}.getOrNull()
