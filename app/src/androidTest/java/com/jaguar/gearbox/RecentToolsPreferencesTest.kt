package com.jaguar.gearbox

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.jaguar.gearbox.data.RecentToolsStore
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

class RecentToolsPreferencesTest {
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val id = "recent_test_${UUID.randomUUID()}"
    private val directory = File(context.cacheDir, id)
    private val file = File(directory, "recent.json")
    private val legacy = context.getSharedPreferences(id, Context.MODE_PRIVATE)
    @After fun cleanup() { directory.deleteRecursively(); context.deleteSharedPreferences(id) }

    @Test fun defaultsAndMigrationPreserveRoutesAndOtherPreferences() {
        legacy.edit().putString("recent_routes", "tool/tip,tool/percentage,tool/tip")
            .putString("sentinel", "keep").commit()
        val store = RecentToolsStore.inFile(file, legacy)
        assertTrue(store.showRecentTools)
        assertTrue(store.recordRecentTools)
        assertEquals(listOf("tool/tip", "tool/percentage"), store.recent)
        assertFalse(legacy.contains("recent_routes"))
        assertEquals("keep", legacy.getString("sentinel", null))
    }

    @Test fun visibilityAndCollectionAreIndependentAndPersisted() {
        val store = RecentToolsStore.inFile(file)
        assertTrue(store.setShowRecentTools(false))
        store.recordOpened("tool/tip")
        assertEquals(listOf("tool/tip"), store.recent)
        assertTrue(store.setRecordRecentTools(false))
        store.recordOpened("tool/percentage")
        assertEquals(listOf("tool/tip"), store.recent)
        val reopened = RecentToolsStore.inFile(file)
        assertFalse(reopened.showRecentTools)
        assertFalse(reopened.recordRecentTools)
        assertEquals(listOf("tool/tip"), reopened.recent)
        assertTrue(reopened.clear())
        assertTrue(reopened.recent.isEmpty())
        assertFalse(reopened.showRecentTools)
        assertFalse(reopened.recordRecentTools)
    }

    @Test fun boundedDeduplicatedAndInvalidRoutesRejected() {
        val store = RecentToolsStore.inFile(file)
        (1..8).forEach { store.recordOpened("tool/test$it") }
        assertEquals(5, store.recent.size)
        store.recordOpened("tool/test5")
        assertEquals("tool/test5", store.recent.first())
        assertEquals(5, store.recent.distinct().size)
        assertFalse(store.recordOpened("../settings"))
        assertEquals(5, store.recent.size)
    }

    @Test fun oldInstancesCannotReenableCollectionOrResurrectClearedRoutes() {
        val first = RecentToolsStore.inFile(file)
        val second = RecentToolsStore.inFile(file)
        first.recordOpened("tool/tip")
        assertEquals(listOf("tool/tip"), second.recent)
        first.setRecordRecentTools(false)
        assertFalse(second.recordRecentTools)
        second.recordOpened("tool/percentage")
        assertFalse(first.recordRecentTools)
        assertEquals(listOf("tool/tip"), first.recent)
        first.clear()
        second.setShowRecentTools(false)
        assertTrue(first.recent.isEmpty())
        assertFalse(first.showRecentTools)
        val persisted = RecentToolsStore.inFile(file)
        assertFalse(persisted.recordRecentTools)
        assertTrue(persisted.recent.isEmpty())
    }

    @Test fun mutationsRebaseOnPersistedPreferencesRatherThanCachedValues() {
        val store = RecentToolsStore.inFile(file)
        file.writeText("""{"version":1,"show":false,"record":false,"routes":[]}""")
        store.recordOpened("tool/tip")
        assertFalse(store.recordRecentTools)
        assertFalse(store.showRecentTools)
        assertTrue(store.recent.isEmpty())
    }

    @Test fun migrationFailureRetainsLegacyKey() {
        directory.mkdirs()
        val blocked = File(directory, "blocked").apply { writeText("keep") }
        legacy.edit().putString("recent_routes", "tool/tip").commit()
        val store = RecentToolsStore.inFile(File(blocked, "recent.json"), legacy)
        assertEquals(listOf("tool/tip"), store.recent)
        assertTrue(legacy.contains("recent_routes"))
        assertFalse(store.setShowRecentTools(false))
        assertTrue(store.showRecentTools)
    }
}
