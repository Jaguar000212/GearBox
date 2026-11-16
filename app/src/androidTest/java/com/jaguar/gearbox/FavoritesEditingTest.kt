package com.jaguar.gearbox

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.jaguar.gearbox.data.FavoritesStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class FavoritesEditingTest {
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val name = "favorites_edit_${UUID.randomUUID()}"
    private val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    @After fun cleanup() { context.deleteSharedPreferences(name) }

    @Test fun batchAddRetainsOrderAndAvoidsDuplicates() {
        val store = FavoritesStore(prefs)
        store.addAll(listOf("one", "two"))
        store.addAll(listOf("two", "three", "three"))
        assertEquals(listOf("one", "two", "three"), FavoritesStore(prefs).favorites.toList())
    }

    @Test fun undoRestoresPositionAndSyncsShortcuts() {
        var shortcuts = emptyList<String>()
        val store = FavoritesStore(prefs) { shortcuts = it }
        store.addAll(listOf("one", "two", "three"))
        store.toggle("two")
        store.restore("two", 1)
        assertEquals(listOf("one", "two", "three"), shortcuts)
        store.restore("two", 0)
        assertEquals(listOf("one", "two", "three"), store.favorites.toList())
    }

    @Test fun undoAfterListChangesClampsPosition() {
        val store = FavoritesStore(prefs)
        store.restore("one", 40)
        assertEquals(listOf("one"), store.favorites.toList())
    }
}
