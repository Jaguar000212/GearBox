package com.jaguar.gearbox

import com.jaguar.gearbox.data.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class SensitiveToolMemoryStoreTest {
    private val context = IsolatedToolTestContext()
    private val store = ToolMemoryStore(context)
    private val routes = ToolMemoryPolicy.sensitiveTextRoutes
    @After fun cleanup() { context.cleanup() }
    private fun consent() = ToolMemorySettings(historyEnabled = true, rememberResults = true,
        rememberModes = true, automaticMemory = routes.associateWith { ToolAutomaticMemory(true, true) })

    @Test fun globalSettingsAloneCannotCaptureSensitiveInputs() {
        assertTrue(store.saveSettings(ToolMemorySettings(historyEnabled = true, rememberResults = true)))
        routes.forEach { route ->
            assertFalse(store.record(route, mapOf("text" to "private"), "output"))
            assertFalse(store.saveResult(route, mapOf("text" to "private"), "output"))
            assertNull(store.loadResult(route))
            assertTrue(store.history(route).isEmpty())
        }
    }

    @Test fun upgradeWithoutPerToolConsentKeepsOldDataButDoesNotRestoreOrCollect() {
        assertTrue(store.saveSettings(consent()))
        routes.forEach { route ->
            assertTrue(store.record(route, mapOf("text" to "old"), "old output"))
            assertTrue(store.saveResult(route, mapOf("text" to "old"), "old output"))
        }
        // Simulate the prior on-disk settings format, which did not contain per-route consent.
        File(context.noBackupFilesDir, "tool_memory_v1/settings.json").writeText(
            """{"version":1,"historyEnabled":true,"historyLimit":20,"rememberResults":true,"rememberModes":true}""")
        val upgraded = ToolMemoryStore(context)
        routes.forEach { route ->
            assertFalse(upgraded.settings().permitsHistory(route))
            assertFalse(upgraded.settings().permitsResult(route))
            assertNull(upgraded.loadResult(route))
            assertNotNull(upgraded.rememberedResult(route))
            assertEquals(1, upgraded.history(route).size)
            assertFalse(upgraded.record(route, emptyMap(), "new"))
            assertFalse(upgraded.saveResult(route, emptyMap(), "new"))
        }
    }

    @Test fun disablingThenReenablingInvalidatesPendingCaptureAndClearIsRouteScoped() {
        assertTrue(store.saveSettings(consent()))
        routes.forEach { route ->
            assertTrue(store.record(route, emptyMap(), "old"))
            assertTrue(store.saveResult(route, emptyMap(), "old"))
        }
        val route = Tools.ROUTE_TEXT_TOOLS
        val historyGeneration = store.historyGeneration(route)
        val resultGeneration = store.resultGeneration(route)
        assertTrue(store.saveSettings(consent().copy(automaticMemory = emptyMap())))
        assertTrue(store.saveSettings(consent()))
        assertFalse(store.record(route, emptyMap(), "stale", historyGeneration))
        assertFalse(store.saveResult(route, emptyMap(), "stale", resultGeneration))
        assertTrue(store.savePreset("Manual", route, mapOf("text" to "manual")))
        assertTrue(store.saveToWorkbench("Manual", route, emptyMap(), "manual"))
        assertTrue(store.clearHistory(route))
        assertTrue(store.clearRememberedResults(route))
        assertNull(store.rememberedResult(route))
        assertTrue(store.history(route).isEmpty())
        assertNotNull(store.rememberedResult(Tools.ROUTE_QR_GENERATOR))
        assertEquals(1, store.history(Tools.ROUTE_QR_GENERATOR).size)
        assertEquals(1, store.presets().size)
        assertEquals(1, store.savedResults().size)
    }

    @Test fun modeMemoryCannotBeUsedToStorePrivateInput() {
        assertTrue(store.saveSettings(consent()))
        routes.forEach { route -> assertFalse(store.saveMode(route, mapOf("text" to "private"))) }
        assertTrue(store.saveMode(Tools.ROUTE_TEXT_TOOLS,
            mapOf("case" to com.jaguar.gearbox.logic.TextCase.entries.first().name)))
        assertTrue(store.saveMode(Tools.ROUTE_HASH_GENERATOR,
            mapOf("algorithm" to com.jaguar.gearbox.logic.TextHashAlgorithm.entries.first().name)))
    }
}
