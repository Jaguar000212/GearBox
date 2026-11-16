package com.jaguar.gearbox

import androidx.test.platform.app.InstrumentationRegistry
import com.jaguar.gearbox.data.ToolMemorySettings
import com.jaguar.gearbox.data.ToolMemoryStore
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

class ToolMemoryStoreTest {
    private val directory = File(InstrumentationRegistry.getInstrumentation().context.cacheDir,
        "tool_memory_test_${UUID.randomUUID()}")
    private val store = ToolMemoryStore.inDirectory(directory)
    private val route = "tool/percentage"
    @After fun cleanup() { directory.deleteRecursively() }

    @Test fun recordingRequiresConsentAndNeverStoresPasswords() {
        assertFalse(store.record(route, mapOf("amount" to "100"), "10"))
        assertTrue(store.saveSettings(ToolMemorySettings(historyEnabled = true)))
        assertFalse(store.record("tool/password_generator", emptyMap(), "secret"))
        assertTrue(store.record(route, mapOf("amount" to "100"), "10"))
        assertEquals("10", store.history(route).single().output)
        assertTrue(store.history("tool/password_generator").isEmpty())
    }

    @Test fun newestFirstDedupeAndLimitReduction() {
        store.saveSettings(ToolMemorySettings(historyEnabled = true, historyLimit = 3))
        (1..5).forEach { assertTrue(store.record(route, mapOf("amount" to "$it"), "$it")) }
        assertEquals(listOf("5", "4", "3"), store.history(route).map { it.output })
        store.record(route, mapOf("amount" to "5"), "5")
        assertEquals(3, store.history(route).size)
        store.saveSettings(store.settings().copy(historyLimit = 1))
        assertEquals(listOf("5"), ToolMemoryStore.inDirectory(directory).history(route).map { it.output })
    }

    @Test fun pausedHistoryRemainsReadableAndCanBeDeleted() {
        store.saveSettings(ToolMemorySettings(historyEnabled = true))
        store.record(route, emptyMap(), "first")
        store.saveSettings(store.settings().copy(historyEnabled = false))
        assertFalse(store.record(route, emptyMap(), "second"))
        val entry = store.history(route).single()
        assertTrue(store.remove(route, entry.id))
        assertTrue(store.history(route).isEmpty())
    }

    @Test fun rememberingModesAndResultsIsIndependent() {
        store.saveSettings(ToolMemorySettings(rememberModes = true))
        assertTrue(store.saveMode(route, mapOf("mode" to "percent")))
        assertFalse(store.saveResult(route, emptyMap(), "100"))
        assertEquals("percent", store.loadMode(route)["mode"])
        store.saveSettings(store.settings().copy(rememberModes = false, rememberResults = true))
        assertTrue(store.loadMode(route).isEmpty())
        assertTrue(store.saveResult(route, mapOf("amount" to "100"), "10"))
        assertEquals("10", store.loadResult(route)?.output)
        assertTrue(store.history(route).isEmpty())
        assertTrue(store.clearRememberedModes())
        assertTrue(store.clearRememberedResults())
        assertNull(store.loadResult(route))
    }

    @Test fun explicitSavesAreBoundedAndIndependentOfAutomaticHistory() {
        assertTrue(store.savePreset("Usual amount", route, mapOf("amount" to "100")))
        assertTrue(store.saveToWorkbench("Estimate", route, mapOf("amount" to "100"), "10"))
        assertEquals("Usual amount", store.presets().single().name)
        assertEquals("10", store.savedResults().single().output)
        assertTrue(store.history(route).isEmpty())
        assertFalse(store.savePreset("Secret", "tool/password_generator", emptyMap()))
        assertFalse(store.saveToWorkbench(" ", route, emptyMap(), "x"))
        repeat(ToolMemoryStore.MAX_SAVED_ITEMS - 1) {
            assertTrue(store.savePreset("Preset $it", route, emptyMap()))
        }
        assertFalse(store.savePreset("Too many", route, emptyMap()))
        assertEquals(ToolMemoryStore.MAX_SAVED_ITEMS, store.presets().size)
        assertTrue(store.clearPresets())
        assertTrue(store.presets().isEmpty())
        assertEquals(1, store.savedResults().size)
    }

    @Test fun clearAndDeleteInvalidatePendingHistoryWritesAcrossInstances() {
        store.saveSettings(ToolMemorySettings(historyEnabled = true))
        val other = ToolMemoryStore.inDirectory(directory)
        val beforeClear = store.historyGeneration(route)
        assertTrue(other.clearHistory(route))
        assertFalse(store.record(route, emptyMap(), "stale", beforeClear))
        val fresh = store.historyGeneration(route)
        assertTrue(store.record(route, emptyMap(), "fresh", fresh))
        val beforeDelete = store.historyGeneration(route)
        assertTrue(other.remove(route, store.history(route).single().id))
        assertFalse(store.record(route, emptyMap(), "stale", beforeDelete))
        val beforeGlobalClear = store.historyGeneration(route)
        assertTrue(other.clearHistory())
        assertFalse(store.record(route, emptyMap(), "stale", beforeGlobalClear))
        assertTrue(store.history(route).isEmpty())
    }

    @Test fun clearingRememberedDataRejectsPendingWritesWithoutAffectingOtherKinds() {
        store.saveSettings(ToolMemorySettings(historyEnabled = true, rememberModes = true, rememberResults = true))
        val oldMode = store.modeGeneration(route)
        val oldResult = store.resultGeneration(route)
        val history = store.historyGeneration(route)
        assertTrue(store.clearRememberedModes())
        assertFalse(store.saveMode(route, mapOf("mode" to "old"), oldMode))
        assertTrue(store.saveResult(route, emptyMap(), "still allowed", oldResult))
        assertTrue(store.clearRememberedResults())
        assertFalse(store.saveResult(route, emptyMap(), "stale", oldResult))
        assertTrue(store.record(route, emptyMap(), "history unaffected", history))
        assertTrue(store.saveMode(route, mapOf("mode" to "new"), store.modeGeneration(route)))
        assertTrue(store.saveResult(route, emptyMap(), "fresh", store.resultGeneration(route)))
        assertEquals("fresh", store.loadResult(route)?.output)
    }

    @Test fun writeFailureIsReportedWithoutChangingOtherFiles() {
        directory.mkdirs()
        val blockedPath = File(directory, "not-a-directory").apply { writeText("keep") }
        val blockedStore = ToolMemoryStore.inDirectory(blockedPath)
        assertFalse(blockedStore.saveSettings(ToolMemorySettings(historyEnabled = true)))
        assertFalse(blockedStore.savePreset("Example", route, emptyMap()))
        assertEquals("keep", blockedPath.readText())
    }

    @Test fun malformedEntryIsSkippedRatherThanRestored() {
        directory.mkdirs()
        File(directory, "percentage.history.json").writeText(
            """{"version":1,"entries":[{"id":"x","timestampMillis":1,"inputs":{"amount":123},"output":"10"}]}""")
        assertTrue(store.history(route).isEmpty())
    }

    @Test fun corruptStorageFailsClosedAndDoesNotTouchOtherFiles() {
        directory.mkdirs()
        File(directory, "settings.json").writeText("not json")
        File(directory, "percentage.history.json").writeText("{\"version\":99,\"entries\":[]}")
        val sentinel = File(directory, "sentinel.txt").apply { writeText("keep") }
        assertEquals(ToolMemorySettings(), store.settings())
        assertTrue(store.history(route).isEmpty())
        assertTrue(store.clearHistory())
        assertEquals("keep", sentinel.readText())
    }
}
