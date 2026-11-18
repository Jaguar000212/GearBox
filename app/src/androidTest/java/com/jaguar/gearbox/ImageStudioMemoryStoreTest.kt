package com.jaguar.gearbox

import com.jaguar.gearbox.data.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class ImageStudioMemoryStoreTest {
    private val context = IsolatedToolTestContext()
    private val store = ToolMemoryStore(context)
    private val route = "tool/image_studio"
    private val mode = mapOf("format" to "PNG", "quality" to "90", "aspectLock" to "true")
    @After fun cleanup() { context.cleanup() }

    @Test fun onlySafeModesCanPersistAndGlobalClearCannotBeUndoneByPendingWrite() {
        assertFalse(store.saveMode(route, mode))
        assertTrue(store.saveSettings(ToolMemorySettings(rememberModes = true, historyEnabled = true,
            rememberResults = true, automaticMemory = mapOf(route to ToolAutomaticMemory(true, true)))))
        assertTrue(store.saveMode(route, mode))
        assertEquals(mode, store.loadMode(route))
        assertFalse(store.saveMode(route, mode + ("uri" to "content://private/image")))
        assertFalse(store.record(route, mode, "image"))
        assertFalse(store.saveResult(route, mode, "image"))
        assertFalse(store.savePreset("Image", route, mode))
        assertFalse(store.saveToWorkbench("Image", route, mode, "image"))
        assertTrue(store.history(route).isEmpty())
        assertNull(store.loadResult(route))
        assertNull(store.rememberedResult(route))
        val token = store.modeGeneration(route)
        assertTrue(store.clearRememberedModes())
        assertTrue(store.loadMode(route).isEmpty())
        assertFalse(store.saveMode(route, mode, expectedGeneration = token))
        assertTrue(store.saveMode(route, mode))
        assertTrue(store.saveSettings(ToolMemorySettings(rememberModes = false)))
        assertTrue(store.loadMode(route).isEmpty())
    }
}
