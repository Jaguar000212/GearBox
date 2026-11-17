package com.jaguar.gearbox

import com.jaguar.gearbox.logic.SessionUndoRedo
import org.junit.Assert.*
import org.junit.Test

class SessionUndoRedoTest {
    private fun history(steps: Int = 20, bytes: Long = 65536) =
        SessionUndoRedo<String>({ it.length.toLong() * 2 }, steps, bytes)

    @Test fun roundTripSnapshotsAndBranchInvalidatesRedo() {
        val initial = history().record("a", "b").record("b", "c")
        val undo = initial.undo("c")!!
        assertEquals("b", undo.value)
        val redo = undo.history.redo("b")!!
        assertEquals("c", redo.value)
        assertEquals("a", undo.history.undo("b")!!.value)
        assertFalse(undo.history.record("b", "d").canRedo)
        assertEquals("b", undo.history.record("b", "d").undo("d")!!.value)
        assertEquals(2, initial.undoCount) // immutable originals are unchanged
    }
    @Test fun noOpKeepsRedoAndResetErasesBothStacks() {
        val undone = history().record("a", "b").undo("b")!!.history
        assertSame(undone, undone.record("a", "a"))
        val reset = undone.reset()
        assertFalse(reset.canUndo)
        assertFalse(reset.canRedo)
        assertEquals(0L, reset.retainedBytes)
        assertNull(reset.undo("a"))
        assertNull(reset.redo("a"))
    }
    @Test fun stepAndPayloadLimitsEvictOldestAndClampRequestedLimits() {
        var stack = history(100, Long.MAX_VALUE)
        repeat(30) { stack = stack.record("$it", "${it + 1}") }
        assertEquals(20, stack.undoCount)
        repeat(20) { index ->
            val step = stack.undo("${30 - index}")!!
            assertEquals("${29 - index}", step.value)
            stack = step.history
            assertTrue(stack.undoCount + stack.redoCount <= 20)
            assertTrue(stack.retainedBytes <= 65536)
        }
        assertFalse(stack.canUndo)
        val bounded = history(bytes = 8).record("aa", "bb").record("bb", "cc").record("cc", "dd")
        assertEquals(2, bounded.undoCount)
        assertEquals(8L, bounded.retainedBytes)
        assertEquals(8L, bounded.undo("dd")!!.history.retainedBytes)
    }
    @Test fun oversizedEditsClearRatherThanRetainUnboundedPayload() {
        val stack = history(bytes = 8).record("a", "b")
        assertFalse(stack.record("b", "large").canUndo)
        assertFalse(stack.record("large", "b").canUndo)
        assertFalse(history(steps = 0).record("a", "b").canUndo)
        assertFalse(SessionUndoRedo<String>({ -1 }).record("a", "b").canUndo)
    }
    @Test fun unequalSizedUndoRedoStayWithinCombinedBudget() {
        val stack = history(bytes = 10).record("a", "bb").record("bb", "cccc")
        val undone = stack.undo("cccc")!!
        assertTrue(undone.history.retainedBytes <= 10)
        assertEquals("bb", undone.value)
        val redone = undone.history.redo("bb")!!
        assertEquals("cccc", redone.value)
        assertTrue(redone.history.retainedBytes <= 10)
    }
}
