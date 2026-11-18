package com.jaguar.gearbox

import com.jaguar.gearbox.data.*
import org.junit.Assert.*
import org.junit.Test

class TextDiffSnapshotTest {
    @Test fun rawTextIncludingLineEndingsIsPreservedAndEmptyInputIsValid() {
        val inputs = mapOf("firstText" to " a\r\nb\r", "secondText" to "")
        assertEquals(inputs, parseTextDiffSnapshot(inputs)!!.toInputs())
        assertNotNull(parseTextDiffSnapshot(mapOf("firstText" to "", "secondText" to "")))
    }
    @Test fun strictSchemaAndComputationalBoundsFailClosed() {
        val valid = mapOf("firstText" to "a", "secondText" to "b")
        assertNull(parseTextDiffSnapshot(valid - "firstText"))
        assertNull(parseTextDiffSnapshot(valid + ("mode" to "unsafe")))
        assertNotNull(parseTextDiffSnapshot(valid + ("firstText" to "a".repeat(16000))))
        assertNull(parseTextDiffSnapshot(valid + ("firstText" to "a".repeat(16001))))
        assertNotNull(parseTextDiffSnapshot(valid + ("secondText" to "\n".repeat(199))))
        assertNull(parseTextDiffSnapshot(valid + ("secondText" to "\n".repeat(200))))
    }
    @Test fun computationallyValidLongDiffDoesNotWeakenStorageLimits() {
        val inputs = mapOf("firstText" to "a".repeat(5000), "secondText" to "b")
        assertNotNull(parseTextDiffSnapshot(inputs))
        assertFalse(ToolMemoryPolicy.validRecord(inputs, "result"))
    }
    @Test fun automaticCaptureRequiresBothGlobalAndSpecificConsentAndModesCannotStoreText() {
        val route = "tool/text_diff"
        assertTrue(ToolMemoryPolicy.isSensitiveTextRoute(route))
        val global = ToolMemorySettings(historyEnabled = true, rememberResults = true)
        assertFalse(global.permitsHistory(route))
        assertFalse(global.permitsResult(route))
        val consent = mapOf(route to ToolAutomaticMemory(historyEnabled = true, rememberResults = true))
        assertFalse(ToolMemorySettings(automaticMemory = consent).permitsHistory(route))
        assertTrue(global.copy(automaticMemory = consent).permitsHistory(route))
        assertTrue(global.copy(automaticMemory = consent).permitsResult(route))
        assertFalse(ToolMemoryPolicy.validMode(route, mapOf("firstText" to "private")))
        assertTrue(ToolMemoryPolicy.validMode(route, emptyMap()))
    }
}
