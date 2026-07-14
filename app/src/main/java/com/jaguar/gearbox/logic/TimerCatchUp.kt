package com.jaguar.gearbox.logic

/** Elapsed/remaining clock state after catching up to the current time. */
data class TimerSnapshot(
    val stopwatchElapsedMillis: Long,
    val timerRemainingMillis: Long,
    val timerRunning: Boolean,
    val timerFinished: Boolean,
)

/**
 * Advances a persisted stopwatch/timer snapshot by however long has passed since [savedAt], so a
 * clock left running doesn't read as frozen just because nobody was around to tick it forward -
 * shared by the Timer/Stopwatch screen and its home-screen widget so both apply the exact same
 * catch-up math instead of two copies quietly drifting apart.
 */
fun catchUpTimerState(
    stopwatchElapsedMillis: Long,
    stopwatchRunning: Boolean,
    timerRemainingMillis: Long,
    timerRunning: Boolean,
    timerFinished: Boolean,
    savedAt: Long,
    now: Long,
): TimerSnapshot {
    if (savedAt <= 0L) {
        return TimerSnapshot(stopwatchElapsedMillis, timerRemainingMillis, timerRunning, timerFinished)
    }

    val gap = (now - savedAt).coerceAtLeast(0L)
    val newElapsed = if (stopwatchRunning) stopwatchElapsedMillis + gap else stopwatchElapsedMillis

    var newRemaining = timerRemainingMillis
    var newRunning = timerRunning
    var newFinished = timerFinished
    if (timerRunning) {
        newRemaining = (timerRemainingMillis - gap).coerceAtLeast(0L)
        if (newRemaining == 0L) {
            newRunning = false
            newFinished = true
        }
    }

    return TimerSnapshot(newElapsed, newRemaining, newRunning, newFinished)
}

/** Compact "H:MM:SS" / "MM:SS" formatting, without the centisecond precision the live screen uses. */
fun formatDurationCompact(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
