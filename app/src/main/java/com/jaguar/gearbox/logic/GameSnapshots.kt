package com.jaguar.gearbox.logic

/** Game imports are data, never instructions to generate a move or change a lifetime tally. */
data class TambolaSnapshot(val calls: List<Int>) {
    val last: Int get() = calls.lastOrNull() ?: 0
    fun inputs() = mapOf("calls" to calls.joinToString(","))
    fun summary() = "Tambola: ${calls.size} of 90 called\nCurrent: $last\nCalled in order: ${calls.joinToString(", ")}"
}

fun parseTambolaSnapshot(inputs: Map<String, String>): TambolaSnapshot? {
    val raw = inputs["calls"] ?: return null
    if (raw.length > 270) return null
    val calls = if (raw.isEmpty()) emptyList() else raw.split(',').map {
        it.toIntOrNull()?.takeIf { number -> number in 1..90 } ?: return null
    }
    if (calls.size > 90 || calls.distinct().size != calls.size) return null
    return TambolaSnapshot(calls)
}

data class TicTacToeSnapshot(val board: List<String>) {
    val nextPlayer: String get() = if (board.count { it == "X" } == board.count { it == "O" }) "X" else "O"
    fun inputs() = mapOf("board" to board.joinToString("") { it.ifEmpty { "." } })
    fun summary(): String {
        val cells = board.map { it.ifEmpty { null } }
        val winner = winningLine(cells)?.let { cells[it[0]] }
        val status = when {
            winner != null -> "$winner wins"
            isBoardFull(cells) -> "Draw"
            else -> "$nextPlayer to play"
        }
        return "Tic-tac-toe: $status\n" + board.chunked(3).joinToString("\n") { row ->
            row.joinToString(" | ") { it.ifEmpty { "·" } }
        }
    }
}

/** Enumerated once from legal play; rejects simultaneous winners and moves made after a win. */
private val reachableTicTacToeBoards: Set<String> by lazy {
    val reachable = mutableSetOf<String>()
    fun visit(board: List<String>, next: String) {
        val key = board.joinToString("") { it.ifEmpty { "." } }
        if (!reachable.add(key)) return
        val cells = board.map { it.ifEmpty { null } }
        if (winningLine(cells) != null || isBoardFull(cells)) return
        board.indices.filter { board[it].isEmpty() }.forEach { index ->
            visit(board.toMutableList().also { it[index] = next }, if (next == "X") "O" else "X")
        }
    }
    visit(List(9) { "" }, "X")
    reachable
}

fun parseTicTacToeSnapshot(inputs: Map<String, String>): TicTacToeSnapshot? {
    val raw = inputs["board"] ?: return null
    if (raw.length != 9 || raw.any { it !in ".XO" } || raw !in reachableTicTacToeBoards) return null
    return TicTacToeSnapshot(raw.map { if (it == '.') "" else it.toString() })
}
