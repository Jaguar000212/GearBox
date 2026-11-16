package com.jaguar.gearbox

import com.jaguar.gearbox.logic.*
import org.junit.Assert.*
import org.junit.Test

class GameSnapshotsTest {
    @Test fun tambolaPreservesOrderAndRejectsInvalidCalls() {
        val snapshot = parseTambolaSnapshot(mapOf("calls" to "90,1,42"))!!
        assertEquals(listOf(90, 1, 42), snapshot.calls)
        assertEquals(42, snapshot.last)
        assertEquals(snapshot, parseTambolaSnapshot(snapshot.inputs()))
        listOf("1,1", "0", "91", "-1", "1,x", "1,").forEach {
            assertNull(it, parseTambolaSnapshot(mapOf("calls" to it)))
        }
        assertNull(parseTambolaSnapshot(emptyMap()))
        assertEquals(0, parseTambolaSnapshot(mapOf("calls" to ""))!!.last)
    }

    @Test fun ticTacToeRestoresValidPositionAndDerivesTurn() {
        val snapshot = parseTicTacToeSnapshot(mapOf("board" to "XO..X....", "currentPlayer" to "X"))!!
        assertEquals("O", snapshot.nextPlayer)
        assertEquals(snapshot, parseTicTacToeSnapshot(snapshot.inputs()))
        assertNotNull(parseTicTacToeSnapshot(mapOf("board" to "XXXOO....")))
        assertTrue(parseTicTacToeSnapshot(mapOf("board" to "XXXOO...."))!!.summary().contains("X wins"))
    }

    @Test fun ticTacToeRejectsInvalidSymbolsCountsAndPostWinPlay() {
        listOf("", ".........X", "Z........", "O........", "XX.......", "XXXOOO...", "XXXOO.O..").forEach {
            assertNull(it, parseTicTacToeSnapshot(mapOf("board" to it)))
        }
        assertNotNull(parseTicTacToeSnapshot(mapOf("board" to ".........")))
        assertNull(parseTicTacToeSnapshot(emptyMap()))
    }
}
