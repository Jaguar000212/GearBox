package com.jaguar.gearbox

import androidx.test.platform.app.InstrumentationRegistry
import com.jaguar.gearbox.data.ScoreboardStore
import com.jaguar.gearbox.data.ScorePlayer
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ScoreboardSafetyTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val key = "scoreboard_safety_test"
    private val prefs get() = context.getSharedPreferences("gearbox_prefs", 0)

    @Before fun clear() { prefs.edit().remove(key).remove("${key}_v2").commit() }

    @Test fun rapidAdjustmentsUseLatestScoreAndRejectOverflow() {
        val store = ScoreboardStore(context, key)
        store.add(ScorePlayer("A", 0))
        repeat(100) { assertTrue(store.adjustScore(0, 1)) }
        assertEquals(100, store.players[0].score)
        store.update(0, ScorePlayer("A", Int.MAX_VALUE))
        assertFalse(store.adjustScore(0, 1))
        assertEquals(Int.MAX_VALUE, store.players[0].score)
        store.update(0, ScorePlayer("A", Int.MIN_VALUE))
        assertFalse(store.adjustScore(0, -1))
    }

    @Test fun legacyMigrationRetainsRawIncludingMalformedRows() {
        val raw = "Alice\u0001-3\u0002broken\u0002Bob\u00014"
        prefs.edit().putString(key, raw).commit()
        val store = ScoreboardStore(context, key)
        assertEquals(listOf(ScorePlayer("Alice", -3), ScorePlayer("Bob", 4)), store.players.toList())
        store.adjustScore(0, 1)
        assertEquals(raw, prefs.getString(key, null))
        assertEquals(-2, ScoreboardStore(context, key).players[0].score)
    }

    @Test fun namesWithLegacySeparatorsAndDuplicatesRoundTrip() {
        val store = ScoreboardStore(context, key)
        val name = "A\u0001B\u0002C"
        store.add(ScorePlayer(name, -1))
        store.add(ScorePlayer(name, 2))
        assertEquals(store.players.toList(), ScoreboardStore(context, key).players.toList())
        store.clear()
        assertTrue(ScoreboardStore(context, key).players.isEmpty())
    }
}
