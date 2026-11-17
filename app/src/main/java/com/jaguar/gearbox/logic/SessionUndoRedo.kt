package com.jaguar.gearbox.logic

/** Immutable, RAM-only history. Keep in remember, never rememberSaveable or persistent storage.
 * T must be an immutable editor snapshot. Reset after imports/restores or external state changes.
 * The budget covers retained undo + redo payloads, excluding the editor's own current value.
 */
class SessionUndoRedo<T> private constructor(
    private val estimateBytes: (T) -> Long,
    private val maxSteps: Int,
    private val maxBytes: Long,
    private val past: List<Entry<T>>,
    private val future: List<Entry<T>>,
) {
    constructor(estimateBytes: (T) -> Long, maxSteps: Int = 20, maxBytes: Long = 64L * 1024) :
        this(estimateBytes, maxSteps.coerceIn(0, 20), maxBytes.coerceIn(0, 64L * 1024), emptyList(), emptyList())

    private data class Entry<T>(val value: T, val bytes: Long)
    val canUndo: Boolean get() = past.isNotEmpty()
    val canRedo: Boolean get() = future.isNotEmpty()
    val undoCount: Int get() = past.size
    val redoCount: Int get() = future.size
    val retainedBytes: Long get() = past.sumOf { it.bytes } + future.sumOf { it.bytes }

    /** Call for an intentional edit/action with both snapshots, before applying the new value.
     * A real edit invalidates redo. Oversized values clear history rather than retaining secrets
     * beyond the budget or offering an operation that cannot safely be redone.
     */
    fun record(before: T, after: T): SessionUndoRedo<T> {
        if (before == after) return this
        val entry = entry(before) ?: return reset()
        if (entry(after) == null || maxSteps == 0) return reset()
        val next = (if (past.lastOrNull()?.value == before) past else past + entry).toMutableList()
        while (next.size > maxSteps || next.sumOf { it.bytes } > maxBytes) next.removeAt(0)
        return state(next, emptyList())
    }

    fun undo(current: T): SessionUndoRedoStep<T>? {
        val target = past.lastOrNull() ?: return null
        val currentEntry = entry(current) ?: return null
        val nextFuture = (future + currentEntry).toMutableList()
        val nextPast = past.dropLast(1).toMutableList()
        trim(nextPast, nextFuture)
        return SessionUndoRedoStep(target.value, state(nextPast, nextFuture))
    }

    fun redo(current: T): SessionUndoRedoStep<T>? {
        val target = future.lastOrNull() ?: return null
        val currentEntry = entry(current) ?: return null
        val nextPast = (past + currentEntry).toMutableList()
        val nextFuture = future.dropLast(1).toMutableList()
        trim(nextFuture, nextPast)
        return SessionUndoRedoStep(target.value, state(nextPast, nextFuture))
    }

    fun reset(): SessionUndoRedo<T> = state(emptyList(), emptyList())

    private fun entry(value: T): Entry<T>? {
        val bytes = estimateBytes(value)
        return if (bytes < 0 || bytes > maxBytes) null else Entry(value, bytes)
    }

    // Keep the most recent reversal; evict the oldest distant state first.
    private fun trim(older: MutableList<Entry<T>>, newer: MutableList<Entry<T>>) {
        while (older.size + newer.size > maxSteps ||
            older.sumOf { it.bytes } + newer.sumOf { it.bytes } > maxBytes) {
            if (older.isNotEmpty()) older.removeAt(0) else newer.removeAt(0)
        }
    }

    private fun state(past: List<Entry<T>>, future: List<Entry<T>>) =
        SessionUndoRedo(estimateBytes, maxSteps, maxBytes, past.toList(), future.toList())
}

data class SessionUndoRedoStep<T>(val value: T, val history: SessionUndoRedo<T>)
