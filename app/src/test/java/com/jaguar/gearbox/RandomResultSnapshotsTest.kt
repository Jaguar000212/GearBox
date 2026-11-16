package com.jaguar.gearbox

import com.jaguar.gearbox.data.RandomResultSnapshots as Snapshots
import org.junit.Assert.*
import org.junit.Test

class RandomResultSnapshotsTest {
    @Test fun typedOutcomesRestoreWithoutParsingDisplayText() {
        assertEquals(true, Snapshots.coin(mapOf("_heads" to "true")))
        assertEquals(false, Snapshots.coin(mapOf("_heads" to "false")))
        assertEquals(359, Snapshots.bottle(mapOf("_angle" to "359")))
        assertEquals(listOf(1, 6), Snapshots.dice(mapOf("dice" to "2", "_dice" to "1,6")))
        assertEquals(Long.MIN_VALUE, Snapshots.number(mapOf("min" to Long.MIN_VALUE.toString(), "max" to Long.MAX_VALUE.toString(), "_number" to Long.MIN_VALUE.toString())))
        assertEquals(1, Snapshots.choice(mapOf("optionA" to "Same", "optionB" to "Same", "_choice" to "1")))
        assertEquals("ROCK" to "SCISSORS", Snapshots.rps(mapOf("playerMove" to "ROCK", "_cpuMove" to "SCISSORS")))
    }

    @Test fun malformedOutcomesAreRejectedRatherThanCoercedOrRerolled() {
        listOf("", "Heads", "TRUE", "1").forEach { assertNull(Snapshots.coin(mapOf("_heads" to it))) }
        listOf("-1", "360", "NaN", "2.5").forEach { assertNull(Snapshots.bottle(mapOf("_angle" to it))) }
        listOf("1", "1,7", "0,6", "a,6", "1,6,").forEach { assertNull(Snapshots.dice(mapOf("dice" to "2", "_dice" to it))) }
        assertNull(Snapshots.dice(mapOf("dice" to "7", "_dice" to "1,1,1,1,1,1,1")))
        assertNull(Snapshots.number(mapOf("min" to "5", "max" to "1", "_number" to "3")))
        assertNull(Snapshots.number(mapOf("min" to "1", "max" to "5", "_number" to "6")))
        assertNull(Snapshots.choice(mapOf("optionA" to "", "optionB" to "B", "_choice" to "1")))
        assertNull(Snapshots.choice(mapOf("optionA" to "A", "optionB" to "B", "_choice" to "2")))
        assertNull(Snapshots.rps(mapOf("playerMove" to "ROCK", "_cpuMove" to "INVALID")))
    }

    @Test fun legacyEntriesWithoutTypedOutcomeHaveNoReplay() {
        assertNull(Snapshots.coin(emptyMap())); assertNull(Snapshots.bottle(emptyMap()))
        assertNull(Snapshots.dice(mapOf("dice" to "1")))
        assertNull(Snapshots.number(mapOf("min" to "1", "max" to "10")))
        assertNull(Snapshots.choice(mapOf("optionA" to "A", "optionB" to "B")))
        assertNull(Snapshots.rps(mapOf("playerMove" to "ROCK")))
    }
}
