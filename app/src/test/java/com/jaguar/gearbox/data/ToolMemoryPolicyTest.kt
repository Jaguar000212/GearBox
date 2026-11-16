package com.jaguar.gearbox.data

import org.junit.Assert.*
import org.junit.Test

class ToolMemoryPolicyTest {
    @Test fun defaultsAreOptInAndLimitsAreBounded() {
        val defaults = ToolMemorySettings()
        assertFalse(defaults.historyEnabled)
        assertFalse(defaults.rememberModes)
        assertFalse(defaults.rememberResults)
        assertTrue(defaults.rememberGameSessions)
        assertEquals(20, defaults.historyLimit)
        assertEquals(1, defaults.copy(historyLimit = Int.MIN_VALUE).normalized().historyLimit)
        assertEquals(100, defaults.copy(historyLimit = Int.MAX_VALUE).normalized().historyLimit)
    }

    @Test fun passwordAndUnsafeRoutesAreRejected() {
        listOf("tool/password_generator", "../settings", "tool/../secret", "", "tool/ABC", "tool/a/b")
            .forEach { assertFalse(it, ToolMemoryPolicy.permitsRoute(it)) }
        assertTrue(ToolMemoryPolicy.permitsRoute("tool/percentage"))
    }

    @Test fun recordsAreBoundedWithoutTruncation() {
        assertTrue(ToolMemoryPolicy.validRecord(mapOf("amount" to "200"), "20"))
        assertFalse(ToolMemoryPolicy.validRecord(mapOf("text" to "a".repeat(4097)), ""))
        assertFalse(ToolMemoryPolicy.validRecord(mapOf("" to "value"), ""))
        assertFalse(ToolMemoryPolicy.validRecord((1..25).associate { "key$it" to "x" }, ""))
        assertFalse(ToolMemoryPolicy.validRecord(mapOf("a" to "a".repeat(4096), "b" to "b".repeat(4096)), ""))
        assertFalse(ToolMemoryPolicy.validRecord(emptyMap(), "a".repeat(4097)))
    }
}
