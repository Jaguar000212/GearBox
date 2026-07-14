package com.jaguar.gearbox.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TicTacToeTest {

    @Test
    fun `detects a winning row`() {
        val board = listOf("X", "X", "X", null, "O", "O", null, null, null)
        assertEquals(listOf(0, 1, 2), winningLine(board))
    }

    @Test
    fun `detects a winning diagonal`() {
        val board = listOf("O", null, "X", null, "O", "X", "X", null, "O")
        assertEquals(listOf(0, 4, 8), winningLine(board))
    }

    @Test
    fun `no winner on an empty or mixed board`() {
        assertNull(winningLine(List(9) { null }))
        assertNull(winningLine(listOf("X", "O", "X", "O", "X", "O", "O", "X", "O")))
    }

    @Test
    fun `board full detection`() {
        assertFalse(isBoardFull(List(9) { null }))
        assertTrue(isBoardFull(List(9) { "X" }))
    }
}
