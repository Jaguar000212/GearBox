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

    @Test fun deadlinePlanNeedsIndependentConsentAndSavedAsOfRemainsUnchanged() {
        val route = "tool/deadline_planner"
        val inputs = mapOf("deadline" to "2027-01-10", "asOf" to "2027-01-01", "ids" to "1",
            "names" to "Private milestone", "durations" to "3")
        val global = ToolMemorySettings(historyEnabled = true, rememberResults = true, rememberModes = true)
        store.saveSettings(global)
        assertFalse(store.record(route, inputs, "2027-01-08 through 2027-01-10"))
        assertFalse(store.saveResult(route, inputs, "plan"))
        assertFalse(store.saveMode(route, inputs))
        val allowed = global.copy(automaticMemory = mapOf(route to ToolAutomaticMemory(true, true)))
        store.saveSettings(allowed)
        assertTrue(store.record(route, inputs, "plan"))
        assertTrue(store.saveResult(route, inputs, "plan"))
        val token = store.historyGeneration(route)
        store.saveSettings(global)
        assertNull(store.loadResult(route))
        assertEquals("2027-01-01", store.rememberedResult(route)?.inputs?.get("asOf"))
        assertEquals(inputs, store.history(route).single().inputs)
        store.saveSettings(allowed)
        assertFalse(store.record(route, inputs, "late", token))
        assertEquals(inputs, store.loadResult(route)?.inputs)
    }

    @Test fun billUpgradeKeepsHistoricalDataButRequiresNewSeparateConsent() {
        val route = "tool/bill_splitter"
        val inputs = mapOf("bill" to "100", "tax" to "0", "tipPercent" to "10", "diners" to "3")
        val global = ToolMemorySettings(historyEnabled = true, rememberResults = true)
        val allowed = global.copy(automaticMemory = mapOf(route to ToolAutomaticMemory(true, true)))
        store.saveSettings(allowed)
        assertTrue(store.record(route, inputs, "110.00"))
        assertTrue(store.saveResult(route, inputs, "110.00"))
        // A pre-shared-expenses install had global switches only, with no bill-specific consent.
        File(context.noBackupFilesDir, "tool_memory_v1/settings.json").writeText(
            """{"version":1,"historyEnabled":true,"historyLimit":20,"rememberResults":true}""")
        val upgraded = ToolMemoryStore(context)
        assertFalse(upgraded.settings().permitsHistory(route))
        assertNull(upgraded.loadResult(route))
        assertEquals(inputs, upgraded.rememberedResult(route)?.inputs)
        assertEquals(inputs, upgraded.history(route).single().inputs)
        assertFalse(upgraded.record(route, inputs, "new"))
        upgraded.saveSettings(allowed)
        assertEquals(inputs, upgraded.loadResult(route)?.inputs)
    }

    @Test fun decisionMatrixHasIndependentConsentAndPreservesRejectedCaptureData() {
        val route = "tool/decision_matrix"
        val inputs = mapOf("optionLabels" to "Private A\nPrivate B", "criterionLabels" to "Cost",
            "weights" to "1", "scores" to "4\n3")
        val global = ToolMemorySettings(historyEnabled = true, rememberResults = true, rememberModes = true)
        store.saveSettings(global)
        assertFalse(store.record(route, inputs, "result"))
        assertFalse(store.saveResult(route, inputs, "result"))
        assertFalse(store.saveMode(route, inputs))
        val allowed = global.copy(automaticMemory = mapOf(route to ToolAutomaticMemory(true, true)))
        store.saveSettings(allowed)
        assertTrue(store.record(route, inputs, "result"))
        assertTrue(store.saveResult(route, inputs, "result"))
        val token = store.resultGeneration(route)
        store.saveSettings(global)
        store.saveSettings(allowed)
        assertFalse(store.saveResult(route, inputs, "late", token))
        assertFalse(store.record(route, inputs + ("optionLabels" to "x".repeat(4097)), "too large"))
        assertEquals(inputs, store.history(route).single().inputs)
        assertEquals("result", store.loadResult(route)?.output)
        assertFalse(store.settings().permitsResult("tool/text_diff"))
    }

    @Test fun textDiffRequiresItsOwnConsentAndNeverTruncatesOversizedCaptures() {
        val route = "tool/text_diff"
        val inputs = mapOf("firstText" to "private first", "secondText" to "private second")
        val global = ToolMemorySettings(historyEnabled = true, rememberResults = true)
        store.saveSettings(global)
        assertFalse(store.record(route, inputs, "comparison"))
        assertFalse(store.saveResult(route, inputs, "comparison"))
        val allowed = global.copy(automaticMemory = mapOf(route to ToolAutomaticMemory(true, true)))
        store.saveSettings(allowed)
        assertTrue(store.record(route, inputs, "comparison"))
        assertTrue(store.saveResult(route, inputs, "comparison"))
        val generation = store.historyGeneration(route)
        store.saveSettings(global)
        store.saveSettings(allowed)
        assertFalse(store.record(route, inputs, "late", generation))
        val tooLarge = inputs + ("firstText" to "x".repeat(4097))
        assertFalse(store.record(route, tooLarge, "comparison"))
        assertFalse(store.saveResult(route, tooLarge, "comparison"))
        assertFalse(store.saveToWorkbench("Large", route, tooLarge, "comparison"))
        assertEquals(inputs, store.history(route).single().inputs)
        assertEquals(inputs, store.loadResult(route)?.inputs)
        assertTrue(store.savedResults().isEmpty())
    }

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
