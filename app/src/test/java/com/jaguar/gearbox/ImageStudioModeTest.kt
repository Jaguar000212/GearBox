package com.jaguar.gearbox

import com.jaguar.gearbox.data.*
import org.junit.Assert.*
import org.junit.Test

class ImageStudioModeTest {
    private val route = "tool/image_studio"
    private val mode = mapOf("format" to "PNG", "quality" to "90", "aspectLock" to "true")
    @Test fun imageCaptureAndRestoreStayForbiddenEvenWithExplicitAutomaticOverrides() {
        assertFalse(ToolMemoryPolicy.permitsRoute(route))
        assertFalse(canRestoreToolSnapshot(route))
        val settings = ToolMemorySettings(historyEnabled = true, rememberResults = true,
            automaticMemory = mapOf(route to ToolAutomaticMemory(true, true)))
        assertFalse(settings.permitsHistory(route))
        assertFalse(settings.permitsResult(route))
        assertFalse(settings.normalized().automaticMemory.containsKey(route))
        assertTrue(ToolMemoryPolicy.permitsModeRoute(route))
    }
    @Test fun modeWhitelistRejectsImageContentAndInvalidSelectors() {
        assertTrue(ToolMemoryPolicy.validMode(route, mode))
        listOf("PNG", "JPEG", "WEBP_LOSSY").forEach {
            assertTrue(validImageStudioMode(mode + mapOf("format" to it, "quality" to "1", "aspectLock" to "false")))
        }
        assertTrue(validImageStudioMode(mode + ("quality" to "100")))
        listOf("0", "101", "090", "+1", " 1", "1.0").forEach { assertFalse(validImageStudioMode(mode + ("quality" to it))) }
        listOf("uri", "path", "pixels", "width", "height", "crop").forEach {
            assertFalse(ToolMemoryPolicy.validMode(route, mode + (it to "private")))
        }
        assertFalse(validImageStudioMode(mode - "aspectLock"))
        assertFalse(validImageStudioMode(mode + ("aspectLock" to "TRUE")))
        assertFalse(validImageStudioMode(mode + ("format" to "WEBP")))
    }
}
