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

    @Test fun colorBlendUsesExistingConsentAndValidatedPaletteModeStorage() {
        val blend = "tool/color_blend"
        val inputs = mapOf("firstHex" to "#000000", "secondHex" to "#FFFFFF", "weight" to "50")
        assertFalse(store.record(blend, inputs, "#808080"))
        assertFalse(store.saveResult(blend, inputs, "#808080"))
        assertTrue(store.saveSettings(ToolMemorySettings(historyEnabled = true, rememberResults = true, rememberModes = true)))
        assertTrue(store.saveMode(blend, inputs))
        assertFalse(store.saveMode(blend, inputs + ("weight" to "101")))
        assertEquals(inputs, store.loadMode(blend))
        assertTrue(store.record(blend, inputs, "#808080"))
        assertTrue(store.saveResult(blend, inputs, "#808080"))
        assertEquals(inputs, store.loadResult(blend)?.inputs)
        assertEquals("#808080", store.history(blend).single().output)
        assertTrue(store.savePreset("Grayscale midpoint", blend, inputs))
        assertEquals(inputs, store.presets().single().inputs)
        assertTrue(store.saveSettings(ToolMemorySettings()))
        assertNull(store.loadResult(blend))
        assertTrue(store.loadMode(blend).isEmpty())
        assertEquals("#808080", store.history(blend).single().output)
    }

    @Test fun individualHistoryDeletionPreservesMalformedCollectionsByteForByte() {
        directory.mkdirs()
        val valid = org.json.JSONObject().put("id", "valid").put("timestampMillis", 1L)
            .put("inputs", org.json.JSONObject().put("amount", "10")).put("output", "10")
        val malformed = org.json.JSONObject(valid.toString()).put("id", "broken").put("inputs", "wrong type")
        val scenarios = listOf(
            org.json.JSONArray().put(valid).put(malformed),
            org.json.JSONArray().put(valid).put(org.json.JSONObject(valid.toString()).put("id", "future").put("extra", "keep")),
            org.json.JSONArray().put(valid).put(valid),
            org.json.JSONArray().also { a -> repeat(101) { a.put(org.json.JSONObject(valid.toString()).put("id", "$it")) } },
        )
        val file = File(directory, "percentage.history.json")
        for (entries in scenarios) {
            val bytes = org.json.JSONObject().put("version", 1).put("entries", entries).toString().toByteArray()
            file.writeBytes(bytes)
            val generation = store.historyGeneration(route)
            assertFalse(store.remove(route, "valid"))
            assertArrayEquals(bytes, file.readBytes())
            assertEquals(generation, store.historyGeneration(route))
        }
        // An explicit clear retains its intentional whole-file semantics, including corrupt rows.
        assertTrue(store.clearHistory(route))
        assertFalse(file.exists())
    }

    @Test fun individualHistoryDeletionKeepsAllOtherValidRowsAndInvalidatesPendingWrites() {
        store.saveSettings(ToolMemorySettings(historyEnabled = true))
        store.record(route, mapOf("amount" to "10"), "10")
        store.record(route, mapOf("amount" to "20"), "20")
        val before = store.history(route)
        val generation = store.historyGeneration(route)
        assertTrue(store.remove(route, before.first().id))
        assertEquals(listOf(before.last()), ToolMemoryStore.inDirectory(directory).history(route))
        assertFalse(store.record(route, mapOf("amount" to "30"), "30", expectedGeneration = generation))
        assertTrue(store.remove(route, "stale"))
        assertEquals(listOf(before.last()), store.history(route))
    }

    @Test fun selectedMutationsRejectUnreadableCollectionWithoutRewritingBytes() {
        for (collection in listOf("workbench.json", "presets.json")) {
            directory.mkdirs()
            val valid = org.json.JSONObject().put("id", "valid").put("name", "valid")
                .put("route", route).put("timestampMillis", 1L)
                .put("inputs", org.json.JSONObject().put("amount", "10")).put("output", "10")
            val invalid = org.json.JSONObject(valid.toString()).put("id", "invalid").put("inputs", "not an object")
            val scenarios = listOf(
                org.json.JSONArray().put(valid).put(invalid),
                org.json.JSONArray().also { array -> repeat(101) { array.put(org.json.JSONObject(valid.toString()).put("id", "id$it")) } },
                org.json.JSONArray().put(valid).put(org.json.JSONObject(valid.toString()).put("id", "unknown").put("futureField", "preserve")),
                org.json.JSONArray().put(valid).put(valid),
            )
            for (items in scenarios) {
                val file = File(directory, collection)
                val bytes = org.json.JSONObject().put("version", 1).put("items", items).toString().toByteArray()
                file.writeBytes(bytes)
                val rename = if (collection == "presets.json") store.renamePreset("valid", "new") else store.renameSavedResult("valid", "new")
                assertFalse(rename)
                assertArrayEquals(bytes, file.readBytes())
                val removed = if (collection == "presets.json") store.removePresets(setOf("valid")) else store.removeSavedResults(setOf("valid"))
                assertFalse(removed)
                assertArrayEquals(bytes, file.readBytes())
                val added = if (collection == "presets.json") store.savePreset("new", route, mapOf("amount" to "20"))
                    else store.saveToWorkbench("new", route, mapOf("amount" to "20"), "20")
                assertFalse(added)
                assertArrayEquals(bytes, file.readBytes())
            }
        }
    }

    @Test fun renameChangesOnlyNameAndKeepsOrderAcrossReload() {
        assertTrue(store.savePreset("setup", route, mapOf("mode" to "one")))
        assertTrue(store.saveToWorkbench("first", route, mapOf("amount" to "10"), "ten"))
        assertTrue(store.saveToWorkbench("second", route, mapOf("amount" to "20"), "twenty"))
        val before = store.savedResults()
        assertTrue(store.renameSavedResult(before.last().id, "  Renamed  "))
        assertEquals(before.map { if (it.id == before.last().id) it.copy(name = "Renamed") else it },
            ToolMemoryStore.inDirectory(directory).savedResults())
        val preset = store.presets().single()
        assertTrue(store.renamePreset(preset.id, "new setup"))
        assertEquals(preset.copy(name = "new setup"), store.presets().single())
        assertFalse(store.renamePreset("stale", "new"))
        assertFalse(store.renameSavedResult(before.first().id, "   "))
        assertFalse(store.renameSavedResult(before.first().id, "x".repeat(81)))
        assertEquals(before.first(), store.savedResults().first())
    }

    @Test fun selectedRemovalIsScopedAtomicAndIgnoresStaleIds() {
        repeat(3) { store.saveToWorkbench("result $it", route, mapOf("n" to "$it"), "$it") }
        store.savePreset("setup", route, mapOf("mode" to "one"))
        val before = store.savedResults()
        val preset = store.presets().single()
        assertTrue(store.removeSavedResults(emptySet()))
        assertEquals(before, store.savedResults())
        assertTrue(store.removeSavedResults(setOf(before.first().id, before.last().id, "stale")))
        assertEquals(listOf(before[1]), ToolMemoryStore.inDirectory(directory).savedResults())
        assertEquals(listOf(preset), store.presets())
        assertTrue(store.removePresets(setOf(before[1].id)))
        assertEquals(listOf(preset), store.presets())
        assertFalse(store.removeSavedResults((1..101).map { "id$it" }.toSet()))
        assertEquals(listOf(before[1]), store.savedResults())
        assertTrue(store.removePresets(setOf(preset.id)))
        assertTrue(store.presets().isEmpty())
        assertEquals(listOf(before[1]), store.savedResults())
    }

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
