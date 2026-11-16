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
    @Test fun sensitiveTextRequiresSeparateConsentEvenWhenGlobalMemoryIsOn() {
        val settings = ToolMemorySettings(historyEnabled = true, rememberResults = true)
        ToolMemoryPolicy.sensitiveTextRoutes.forEach { route ->
            assertTrue(ToolMemoryPolicy.isSensitiveTextRoute(route))
            assertFalse(settings.permitsHistory(route))
            assertFalse(settings.permitsResult(route))
            assertEquals(ToolAutomaticMemory(), settings.automaticMemoryFor(route))
        }
        assertTrue(settings.permitsHistory("tool/percentage"))
        assertTrue(settings.permitsResult("tool/percentage"))
    }

    @Test fun routeAndMemoryKindsAreIndependentAndGlobalOffStillWins() {
        val route = "tool/text_tools"
        val historyOnly = ToolMemorySettings(historyEnabled = true, rememberResults = true,
            automaticMemory = mapOf(route to ToolAutomaticMemory(historyEnabled = true)))
        assertTrue(historyOnly.permitsHistory(route))
        assertFalse(historyOnly.permitsResult(route))
        assertFalse(historyOnly.permitsHistory("tool/hash_generator"))
        assertFalse(historyOnly.copy(historyEnabled = false).permitsHistory(route))
        val resultOnly = historyOnly.copy(automaticMemory = mapOf(route to ToolAutomaticMemory(rememberResults = true)))
        assertFalse(resultOnly.permitsHistory(route))
        assertTrue(resultOnly.permitsResult(route))
        assertFalse(resultOnly.copy(rememberResults = false).permitsResult(route))
    }

    @Test fun explicitOverridesCannotEnablePasswordsOrUnsafeRoutes() {
        val overrides = listOf("tool/password_generator", "../private", "tool/ABC")
            .associateWith { ToolAutomaticMemory(true, true) }
        val settings = ToolMemorySettings(historyEnabled = true, rememberResults = true, automaticMemory = overrides)
        overrides.keys.forEach { route ->
            assertFalse(settings.permitsHistory(route)); assertFalse(settings.permitsResult(route))
        }
        assertTrue(settings.normalized().automaticMemory.isEmpty())
    }

    @Test fun sensitiveModeStorageAllowsOnlyEnumeratedSelectorsNotContent() {
        assertTrue(ToolMemoryPolicy.validMode("tool/text_tools", mapOf("case" to "UPPER")))
        assertTrue(ToolMemoryPolicy.validMode("tool/hash_generator", mapOf("algorithm" to "SHA256")))
        assertFalse(ToolMemoryPolicy.validMode("tool/text_tools", mapOf("case" to "private text")))
        assertFalse(ToolMemoryPolicy.validMode("tool/text_tools", mapOf("text" to "secret")))
        assertFalse(ToolMemoryPolicy.validMode("tool/hash_generator", mapOf("algorithm" to "SHA256", "text" to "secret")))
        assertFalse(ToolMemoryPolicy.validMode("tool/qr_generator", mapOf("text" to "secret")))
        assertFalse(ToolMemoryPolicy.validMode("tool/password_generator", emptyMap()))
    }

}
