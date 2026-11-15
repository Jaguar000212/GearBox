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

    @Before fun clear() { prefs.edit().remove(key).remove("${key}_v2").remove("${key}_v2_recovery").commit() }

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

    @Test fun verifiedLegacyMigrationRetiresOldCopyBeforeEdits() {
        prefs.edit().putString(key, "Alice\u0001-3\u0002Bob\u00014").commit()
        val store = ScoreboardStore(context, key)
        assertFalse(prefs.contains(key))
        assertFalse(store.hasRetainedLegacyData)
        assertEquals(store.players.toList(), ScoreboardStore(context, key).players.toList())
        store.removeAt(0)
        assertEquals(listOf(ScorePlayer("Bob", 4)), ScoreboardStore(context, key).players.toList())
        store.resetScores()
        assertEquals(listOf(ScorePlayer("Bob", 0)), ScoreboardStore(context, key).players.toList())
        store.clear()
        assertTrue(ScoreboardStore(context, key).players.isEmpty())
        assertFalse(prefs.contains(key))
    }

    @Test fun malformedLegacyRemainsDisclosedAfterResetAndClear() {
        val raw = "Alice\u0001-3\u0002broken"
        prefs.edit().putString(key, raw).commit()
        val store = ScoreboardStore(context, key)
        store.resetScores()
        assertEquals(0, ScoreboardStore(context, key).players[0].score)
        store.removeAt(0)
        store.clear()
        val reopened = ScoreboardStore(context, key)
        assertTrue(reopened.players.isEmpty())
        assertTrue(reopened.hasRetainedLegacyData)
        assertEquals(raw, prefs.getString(key, null))
    }

    @Test fun differingLegacyAndV2DataIsNotDeleted() {
        prefs.edit().putString(key, "Old\u00011")
            .putString("${key}_v2", "[{\"name\":\"New\",\"score\":2}]").commit()
        val store = ScoreboardStore(context, key)
        assertEquals(listOf(ScorePlayer("New", 2)), store.players.toList())
        assertTrue(store.hasRetainedLegacyData)
        store.clear()
        assertEquals("Old\u00011", prefs.getString(key, null))
        assertTrue(ScoreboardStore(context, key).players.isEmpty())
    }

    @Test fun malformedV2IsReadOnlyAndNeverReplaced() {
        val broken = "[{broken post-migration data"
        prefs.edit().putString("${key}_v2", broken).commit()
        val store = ScoreboardStore(context, key)
        assertTrue(store.hasUnreadableSavedData)
        store.add(ScorePlayer("New", 7))
        assertEquals(broken, prefs.getString("${key}_v2", null))
        assertTrue(store.isReadOnly)
        assertTrue(store.players.isEmpty())
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
