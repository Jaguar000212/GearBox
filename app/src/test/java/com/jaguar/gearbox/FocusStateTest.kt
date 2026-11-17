package com.jaguar.gearbox

import com.jaguar.gearbox.focus.*
import org.junit.Assert.*
import org.junit.Test

class FocusStateTest {
    private val interval = FocusConfig(FocusMode.INTERVAL, 2, 1, 3, 3, 2)
    @Test fun phasesAreBoundedAndOnlyBetweenWorkRounds() {
        val phases = focusPhases(FocusConfig(rounds = 5, longBreakEvery = 2))
        assertEquals(9, phases.size)
        assertEquals(FocusPhaseKind.LONG_BREAK, phases[3].kind)
        assertEquals(FocusPhaseKind.LONG_BREAK, phases[7].kind)
        assertEquals(FocusPhaseKind.WORK, phases.last().kind)
        assertEquals(1, focusPhases(FocusConfig(rounds = 1)).size)
        assertEquals(197, focusPhases(FocusConfig(rounds = 99)).size)
        assertFalse(focusPhases(interval).any { it.kind == FocusPhaseKind.LONG_BREAK })
    }
    @Test fun exactAndDelayedCatchupUseElapsedClockAndDoNotBurstAlerts() {
        val start = startFocus(interval, 0, 4, 0)
        val boundary = advanceFocus(start, 2000, 4)
        assertEquals(1, boundary.phaseIndex)
        assertEquals(1000L, boundary.remainingMillis)
        val late = advanceFocus(start, 3500, 4)
        assertEquals(2, late.phaseIndex)
        assertEquals(1500L, late.remainingMillis)
        val claimed = claimFocusBoundary(late)!!
        assertEquals(2, claimed.alertedPhase)
        assertNull(claimFocusBoundary(claimed))
        val finished = advanceFocus(start, Long.MAX_VALUE, 4)
        assertEquals(FocusStatus.FINISHED, finished.status)
        assertEquals(0L, finished.remainingMillis)
        assertNull(claimFocusBoundary(claimFocusBoundary(finished)!!))
    }
    @Test fun pauseResumeIsExplicitAndFrozenDuringPause() {
        val start = startFocus(interval, 100, 1, 7)
        val paused = pauseFocus(start, 1100, 1)
        assertEquals(FocusStatus.PAUSED, paused.status)
        assertEquals(1000L, paused.remainingMillis)
        assertEquals(paused, advanceFocus(paused, 100000, 1))
        val resumed = resumeFocus(paused, 100000, 1)
        assertEquals(start.generation + 1, resumed.generation)
        assertEquals(1, advanceFocus(resumed, 101000, 1).phaseIndex)
    }
    @Test fun rebootUnknownClockAndElapsedReversalInterruptWithoutRestart() {
        val start = startFocus(interval, 5000, 3, 0)
        for ((elapsed, boot) in listOf(6000L to 4, 4999L to 3, 6000L to -1)) {
            val stopped = advanceFocus(start, elapsed, boot)
            assertEquals(FocusStatus.INTERRUPTED, stopped.status)
            assertEquals(start.remainingMillis, stopped.remainingMillis)
            assertNull(claimFocusBoundary(stopped))
            assertEquals(stopped, advanceFocus(stopped, 900000, 5))
        }
    }
    @Test fun invalidConfigurationAndPersistedStatesFailClosed() {
        for (bad in listOf(interval.copy(workSeconds = 0), interval.copy(shortBreakSeconds = 21601),
            interval.copy(longBreakSeconds = -1), interval.copy(rounds = 100), interval.copy(longBreakEvery = 21))) {
            assertFalse(bad.isValid())
            assertThrows(IllegalArgumentException::class.java) { focusPhases(bad) }
        }
        assertThrows(IllegalArgumentException::class.java) { startFocus(interval, 0, -1, 0) }
        val start = startFocus(interval, 0, 1, 0)
        assertTrue(validFocusState(start))
        assertFalse(validFocusState(start.copy(phaseIndex = 1000)))
        assertFalse(validFocusState(start.copy(remainingMillis = 0)))
        assertFalse(validFocusState(start.copy(remainingMillis = Long.MAX_VALUE)))
        assertFalse(validFocusState(start.copy(alertedPhase = 100)))
        assertFalse(validFocusState(start.copy(bootCount = -1)))
    }
}
