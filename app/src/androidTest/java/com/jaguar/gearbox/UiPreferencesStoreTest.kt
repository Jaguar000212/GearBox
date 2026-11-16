package com.jaguar.gearbox

import androidx.test.platform.app.InstrumentationRegistry
import com.jaguar.gearbox.data.UiPreferencesStore
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

class UiPreferencesStoreTest {
    private val directory = File(InstrumentationRegistry.getInstrumentation().context.noBackupFilesDir,
        "ui_preferences_test_${UUID.randomUUID()}").also { it.mkdirs() }
    @After fun cleanup() { directory.deleteRecursively() }

    @Test fun defaultsAreConservativeAndStoresObserveEachOther() {
        val first = UiPreferencesStore.inDirectory(directory)
        val second = UiPreferencesStore.inDirectory(directory)
        assertFalse(first.preferences.value.reduceMotion)
        assertTrue(first.preferences.value.showActiveClockShortcut)
        assertTrue(first.setReduceMotion(true))
        assertTrue(second.preferences.value.reduceMotion)
        assertTrue(second.setShowActiveClockShortcut(false))
        assertTrue(first.preferences.value.reduceMotion)
        assertFalse(first.preferences.value.showActiveClockShortcut)
    }

    @Test fun preferencesPersistAndKeepUnrelatedChoice() {
        val first = UiPreferencesStore.inDirectory(directory)
        assertTrue(first.setReduceMotion(true))
        assertTrue(first.setShowActiveClockShortcut(false))
        val stored = File(directory, "ui_preferences_v1.json").readText()
        assertTrue(stored.contains("\"reduceMotion\":true"))
        assertTrue(stored.contains("\"showActiveClockShortcut\":false"))
        val another = UiPreferencesStore.inDirectory(directory)
        assertTrue(another.setReduceMotion(false))
        assertFalse(first.showActiveClockShortcut)
        assertFalse(first.reduceMotion)
    }

    @Test fun malformedAndOversizedFilesFallBackSafely() {
        File(directory, "ui_preferences_v1.json").writeText("not json")
        val malformed = UiPreferencesStore.inDirectory(directory)
        assertFalse(malformed.reduceMotion)
        assertTrue(malformed.showActiveClockShortcut)
        val other = File(directory, "oversized").also { it.mkdirs() }
        File(other, "ui_preferences_v1.json").writeText(" ".repeat(5000))
        assertFalse(UiPreferencesStore.inDirectory(other).reduceMotion)
    }
}
