package com.jaguar.gearbox

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.jaguar.gearbox.data.*
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class ScoreSnapshotsTest {
    @Test fun scoreboardRoundTripAndStrictValidation() {
        val players = listOf(ScorePlayer("A\n\"B", Int.MIN_VALUE), ScorePlayer("A", Int.MAX_VALUE))
        assertEquals(players, ScoreSnapshots.readScoreboard(ScoreSnapshots.scoreboard(players)!!))
        assertNull(ScoreSnapshots.readScoreboard(mapOf("version" to "1", "players" to "[{\"name\":\"A\",\"score\":1.5}]")))
        assertNull(ScoreSnapshots.readScoreboard(mapOf("version" to "2", "players" to "[]")))
        assertNull(ScoreSnapshots.readScoreboard(mapOf("version" to "1", "players" to "[{\"name\":\"A\",\"score\":2147483648}]")))
        assertNull(ScoreSnapshots.scoreboard(List(101) { ScorePlayer("A", 0) }))
    }

    @Test fun chessRejectsInvalidScoreTotalsAndNonFiniteValues() {
        val match = ChessMatch("White", "Black", 1.5f, 0.5f, 2)
        assertEquals(match, ScoreSnapshots.readChess(ScoreSnapshots.chess(match)!!))
        assertNull(ScoreSnapshots.chess(match.copy(whiteScore = Float.NaN)))
        assertNull(ScoreSnapshots.chess(match.copy(whiteScore = 1.25f)))
        assertNull(ScoreSnapshots.chess(match.copy(gamesPlayed = 3)))
        assertNull(ScoreSnapshots.chess(match.copy(blackScore = -0.5f)))
    }

    @Test fun restoreReplacesEntireScoreboardWithoutErasingLegacyRecovery() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val key = "snapshot_${UUID.randomUUID()}"
        val prefs = context.getSharedPreferences("gearbox_prefs", Context.MODE_PRIVATE)
        try {
            prefs.edit().putString(key, "unreadable recovery").commit()
            val store = ScoreboardStore(context, key)
            store.add(ScorePlayer("Current", 7))
            assertFalse(store.restoreSnapshot(mapOf("version" to "wrong")))
            assertEquals(listOf(ScorePlayer("Current", 7)), store.players.toList())
            val snapshot = ScoreSnapshots.scoreboard(listOf(ScorePlayer("Restored", -2)))!!
            assertTrue(store.restoreSnapshot(snapshot))
            assertEquals(listOf(ScorePlayer("Restored", -2)), ScoreboardStore(context, key).players.toList())
            assertEquals("unreadable recovery", prefs.getString(key, null))
        } finally { prefs.edit().remove(key).remove("${key}_v2").commit() }
    }

    @Test fun unreadableCurrentDataRemainsReadOnlyOnRestore() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val key = "snapshot_${UUID.randomUUID()}"
        val prefs = context.getSharedPreferences("gearbox_prefs", Context.MODE_PRIVATE)
        try {
            prefs.edit().putString("${key}_v2", "broken").commit()
            val store = ScoreboardStore(context, key)
            assertFalse(store.restoreSnapshot(ScoreSnapshots.scoreboard(emptyList())!!))
            assertEquals("broken", prefs.getString("${key}_v2", null))
        } finally { prefs.edit().remove(key).remove("${key}_v2").commit() }
    }
    @Test fun clearingAfterCapturePreventsDelayedScoreSnapshotsReturning() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val directory = java.io.File(context.cacheDir, "score_capture_${UUID.randomUUID()}")
        val memory = ToolMemoryStore.inDirectory(directory)
        try {
            assertTrue(memory.saveSettings(ToolMemorySettings(historyEnabled = true)))
            listOf(Tools.ROUTE_SCOREBOARD, Tools.ROUTE_CHESS_SCOREBOARD).forEach { route ->
                val captured = if (route == Tools.ROUTE_SCOREBOARD)
                    ScoreSnapshots.scoreboard(listOf(ScorePlayer("A", 1)))!!
                else ScoreSnapshots.chess(ChessMatch("A", "B", 1f, 0f, 1))!!
                val generation = memory.historyGeneration(route)
                assertTrue(memory.clearHistory(route))
                assertFalse(memory.record(route, captured, "A: 1", expectedGeneration = generation))
                assertTrue(memory.history(route).isEmpty())
                assertTrue(memory.record(route, captured, "A: 1", expectedGeneration = memory.historyGeneration(route)))
                val beforeDelete = memory.historyGeneration(route)
                assertTrue(memory.remove(route, memory.history(route).single().id))
                assertFalse(memory.record(route, captured, "A: 1", expectedGeneration = beforeDelete))
                assertTrue(memory.history(route).isEmpty())
            }
        } finally { directory.deleteRecursively() }
    }

}
