package com.jaguar.gearbox.logic

private val WIN_LINES = listOf(
    listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8), // rows
    listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8), // columns
    listOf(0, 4, 8), listOf(2, 4, 6), // diagonals
)

/** Returns the winning line (as board indices) if [board] (size 9, null = empty cell) has one. */
fun winningLine(board: List<String?>): List<Int>? =
    WIN_LINES.firstOrNull { line -> line.all { board[it] != null && board[it] == board[line[0]] } }

fun isBoardFull(board: List<String?>): Boolean = board.all { it != null }
