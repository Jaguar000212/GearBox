package com.jaguar.gearbox.logic

/** Read-only presentation; never resumes, resets, or selects a clock mode. */
fun homeClockSummary(state: ClockState): String? {
    val statuses = buildList {
        when {
            state.timerRunning -> add("Timer running")
            state.timerFinished -> add("Timer finished")
            state.timerRemainingMillis > 0 -> add("Timer paused")
        }
        when {
            state.stopwatchRunning -> add("Stopwatch running")
            state.stopwatchElapsedMillis > 0 -> add("Stopwatch paused")
        }
    }
    return statuses.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}
