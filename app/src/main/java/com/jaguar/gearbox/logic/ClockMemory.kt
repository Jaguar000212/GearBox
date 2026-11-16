package com.jaguar.gearbox.logic

/** Only reusable configuration crosses the history boundary, never running/elapsed clock state. */
data class ClockSetup(val mode: String, val minutes: String, val seconds: String)

fun parseClockSetup(values: Map<String, String>): ClockSetup? {
    val mode = values["mode"]?.takeIf { it == "TIMER" || it == "STOPWATCH" } ?: return null
    val minutes = values["minutes"]?.toLongOrNull()?.takeIf { it in 0..999 } ?: return null
    val seconds = values["seconds"]?.toLongOrNull()?.takeIf { it in 0..59 } ?: return null
    if (mode == "TIMER" && minutes == 0L && seconds == 0L) return null
    return ClockSetup(mode, minutes.toString(), seconds.toString())
}

fun clockHasProgress(state: ClockState): Boolean = state.timerRunning || state.stopwatchRunning ||
    state.timerRemainingMillis > 0 || state.stopwatchElapsedMillis > 0 || state.laps.isNotEmpty()

/** Refuse rather than silently discard a paused or running clock. Never starts either clock. */
fun restoreClockSetup(state: ClockState, setup: ClockSetup): ClockState? {
    if (state.mode == setup.mode && state.minutesInput == setup.minutes && state.secondsInput == setup.seconds) return state
    if (clockHasProgress(state)) return null
    return state.copy(mode = setup.mode, minutesInput = setup.minutes, secondsInput = setup.seconds,
        timerRemainingMillis = 0, timerFinished = false)
}

/** Implicit preferences must not override a mode chosen by a notification or shortcut. */
fun mayImportClockMode(explicitImport: Boolean): Boolean = explicitImport

/** Preference hydration is only safe for an ordinary, fully idle launch. */
fun applyClockPreferences(
    state: ClockState,
    navigationTarget: String?,
    requestReset: Boolean,
    rememberModes: Boolean,
    remembered: ClockSetup?,
): ClockState {
    if (navigationTarget != null || requestReset || clockHasProgress(state) || state.timerFinished) return state
    val setup = if (rememberModes) remembered ?: return state else ClockSetup("STOPWATCH", "5", "0")
    return restoreClockSetup(state, setup) ?: state
}
