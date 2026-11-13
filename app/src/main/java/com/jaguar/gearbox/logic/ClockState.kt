package com.jaguar.gearbox.logic

/** Persisted state shared by the screen, widgets and alarm receiver. */
data class ClockState(
    val mode: String = "STOPWATCH",
    val stopwatchElapsedMillis: Long = 0,
    val stopwatchRunning: Boolean = false,
    val laps: List<Long> = emptyList(),
    val timerRemainingMillis: Long = 0,
    val timerRunning: Boolean = false,
    val timerFinished: Boolean = false,
    val minutesInput: String = "5",
    val secondsInput: String = "0",
    val savedAt: Long = 0,
    val savedElapsed: Long = -1,
    val bootCount: Int = -1,
    val generation: Long = 0,
    val alertedGeneration: Long = 0,
)

/** Use a monotonic clock within one boot; wall time is only a reboot/legacy fallback. */
fun advanceClockState(state: ClockState, now: Long, elapsed: Long, bootCount: Int): ClockState {
    val gap = when {
        state.savedElapsed >= 0 && state.bootCount >= 0 && state.bootCount == bootCount ->
            (elapsed - state.savedElapsed).coerceAtLeast(0)
        state.savedAt > 0 -> (now - state.savedAt).coerceAtLeast(0)
        else -> 0
    }
    val remaining = if (state.timerRunning) {
        (state.timerRemainingMillis.coerceAtLeast(0) - gap).coerceAtLeast(0)
    } else state.timerRemainingMillis.coerceAtLeast(0)
    val elapsedMillis = state.stopwatchElapsedMillis.coerceAtLeast(0)
    val sw = if (state.stopwatchRunning) {
        elapsedMillis + gap.coerceAtMost(Long.MAX_VALUE - elapsedMillis)
    } else elapsedMillis
    return state.copy(
        stopwatchElapsedMillis = sw,
        timerRemainingMillis = remaining,
        timerRunning = state.timerRunning && remaining > 0,
        timerFinished = state.timerFinished || (state.timerRunning && remaining == 0L),
        savedAt = now, savedElapsed = elapsed, bootCount = bootCount,
    )
}

fun timerDurationMillis(minutes: String, seconds: String): Long? {
    val m = minutes.trim().toLongOrNull() ?: return null
    val s = seconds.trim().toLongOrNull() ?: return null
    if (m !in 0..999 || s !in 0..59) return null
    return ((m * 60 + s) * 1000).takeIf { it > 0 }
}

/** A completion can be claimed once per started/resumed session, never for a stale alarm. */
fun claimTimerCompletion(state: ClockState, expectedGeneration: Long? = null): ClockState? {
    if (expectedGeneration != null && expectedGeneration != state.generation) return null
    if (!state.timerFinished || state.timerRunning || state.generation <= state.alertedGeneration) return null
    return state.copy(alertedGeneration = state.generation)
}

fun toggleCountdown(state: ClockState, allowNewDuration: Boolean = true): ClockState {
    if (state.timerRunning) return state.copy(timerRunning = false)
    val remaining = state.timerRemainingMillis.takeIf { it > 0 }
        ?: (if (allowNewDuration) timerDurationMillis(state.minutesInput, state.secondsInput) else null)
        ?: return state
    return state.copy(
        timerRemainingMillis = remaining, timerRunning = true, timerFinished = false,
        generation = if (state.generation == Long.MAX_VALUE) 1 else state.generation + 1,
        alertedGeneration = 0,
    )
}

fun resetCountdown(state: ClockState): ClockState = state.copy(
    timerRunning = false, timerRemainingMillis = 0, timerFinished = false,
)
