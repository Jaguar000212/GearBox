package com.jaguar.gearbox.focus

enum class FocusMode { POMODORO, INTERVAL }
enum class FocusStatus { IDLE, RUNNING, PAUSED, FINISHED, INTERRUPTED }
enum class FocusPhaseKind { WORK, SHORT_BREAK, LONG_BREAK }

data class FocusConfig(
    val mode: FocusMode = FocusMode.POMODORO,
    val workSeconds: Int = 1500,
    val shortBreakSeconds: Int = 300,
    val longBreakSeconds: Int = 900,
    val rounds: Int = 4,
    val longBreakEvery: Int = 4,
) {
    fun isValid(): Boolean = workSeconds in 1..21600 && shortBreakSeconds in 1..21600 &&
        longBreakSeconds in 1..21600 && rounds in 1..99 && longBreakEvery in 1..20
}
data class FocusPhase(val kind: FocusPhaseKind, val round: Int, val durationMillis: Long)
fun focusPhases(config: FocusConfig): List<FocusPhase> {
    require(config.isValid()) { "Invalid focus configuration" }
    return buildList {
        for (round in 1..config.rounds) {
            add(FocusPhase(FocusPhaseKind.WORK, round, config.workSeconds * 1000L))
            if (round < config.rounds) {
                val long = config.mode == FocusMode.POMODORO && round % config.longBreakEvery == 0
                add(FocusPhase(if (long) FocusPhaseKind.LONG_BREAK else FocusPhaseKind.SHORT_BREAK,
                    round, (if (long) config.longBreakSeconds else config.shortBreakSeconds) * 1000L))
            }
        }
    }
}
data class FocusState(
    val config: FocusConfig = FocusConfig(),
    val status: FocusStatus = FocusStatus.IDLE,
    val phaseIndex: Int = 0,
    val remainingMillis: Long = 0,
    val savedElapsed: Long = -1,
    val bootCount: Int = -1,
    val generation: Long = 0,
    val alertedPhase: Int = -1,
) {
    val phase: FocusPhase get() = focusPhases(config)[phaseIndex]
    val isActive: Boolean get() = status in setOf(FocusStatus.RUNNING, FocusStatus.PAUSED, FocusStatus.INTERRUPTED)
}

/** Never uses wall time to restart/advance a session after reboot or monotonic discontinuity. */
fun advanceFocus(state: FocusState, elapsed: Long, bootCount: Int): FocusState {
    if (state.status != FocusStatus.RUNNING) return state
    if (state.bootCount < 0 || bootCount < 0 || state.bootCount != bootCount ||
        state.savedElapsed < 0 || elapsed < state.savedElapsed) {
        return state.copy(status = FocusStatus.INTERRUPTED, savedElapsed = elapsed, bootCount = bootCount)
    }
    val phases = focusPhases(state.config)
    var gap = elapsed - state.savedElapsed
    var index = state.phaseIndex
    var remaining = state.remainingMillis
    while (gap >= remaining) {
        gap -= remaining
        if (index == phases.lastIndex) return state.copy(status = FocusStatus.FINISHED,
            phaseIndex = index, remainingMillis = 0, savedElapsed = elapsed, bootCount = bootCount)
        index++
        remaining = phases[index].durationMillis
    }
    return state.copy(phaseIndex = index, remainingMillis = remaining - gap, savedElapsed = elapsed)
}

fun startFocus(config: FocusConfig, elapsed: Long, bootCount: Int, generation: Long): FocusState {
    require(config.isValid() && elapsed >= 0 && bootCount >= 0) { "Focus configuration or boot clock is unavailable" }
    return FocusState(config, FocusStatus.RUNNING, 0, config.workSeconds * 1000L, elapsed,
        bootCount, if (generation == Long.MAX_VALUE) 1 else generation + 1, 0)
}
fun pauseFocus(state: FocusState, elapsed: Long, bootCount: Int): FocusState {
    val current = advanceFocus(state, elapsed, bootCount)
    return if (current.status == FocusStatus.RUNNING) current.copy(status = FocusStatus.PAUSED) else current
}
fun resumeFocus(state: FocusState, elapsed: Long, bootCount: Int): FocusState {
    require(elapsed >= 0 && bootCount >= 0) { "Boot clock is unavailable" }
    return if (state.status == FocusStatus.PAUSED || state.status == FocusStatus.INTERRUPTED)
        state.copy(status = FocusStatus.RUNNING, savedElapsed = elapsed, bootCount = bootCount,
            generation = if (state.generation == Long.MAX_VALUE) 1 else state.generation + 1)
    else state
}
fun claimFocusBoundary(state: FocusState): FocusState? {
    val token = if (state.status == FocusStatus.FINISHED) focusPhases(state.config).size else state.phaseIndex
    if (state.status !in setOf(FocusStatus.RUNNING, FocusStatus.FINISHED) || token <= state.alertedPhase) return null
    return state.copy(alertedPhase = token)
}

/** Validates decoded state before any phase indexing, scheduling or catch-up work. */
fun validFocusState(state: FocusState): Boolean {
    if (!state.config.isValid()) return false
    val phases = focusPhases(state.config)
    if (state.phaseIndex !in phases.indices || state.remainingMillis !in 0..phases[state.phaseIndex].durationMillis ||
        state.savedElapsed < -1 || state.bootCount < -1 || state.generation < 0) return false
    val maxAlert = if (state.status == FocusStatus.FINISHED) phases.size else state.phaseIndex
    if (state.alertedPhase !in -1..maxAlert) return false
    if (state.isActive && (state.remainingMillis <= 0 || state.generation <= 0 || state.savedElapsed < 0)) return false
    if (state.status == FocusStatus.RUNNING && state.bootCount < 0) return false
    if (state.status == FocusStatus.FINISHED && (state.remainingMillis != 0L || state.phaseIndex != phases.lastIndex)) return false
    if (state.status == FocusStatus.IDLE && (state.phaseIndex != 0 || state.remainingMillis != 0L)) return false
    return true
}
