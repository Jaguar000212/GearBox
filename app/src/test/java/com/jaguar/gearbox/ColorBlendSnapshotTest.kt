package com.jaguar.gearbox

import com.jaguar.gearbox.data.*
import org.junit.Assert.*
import org.junit.Test

class ColorBlendSnapshotTest {
    private val valid = mapOf("firstHex" to "#aabbcc", "secondHex" to "#123456", "weight" to "50")
    @Test fun canonicalSnapshotRoundTripsWithoutAnOutputField() {
        val snapshot = parseColorBlendSnapshot(valid)!!
        assertEquals("#AABBCC", snapshot.firstHex)
        assertEquals(50, snapshot.weight)
        assertEquals(snapshot, parseColorBlendSnapshot(snapshot.toInputs()))
        assertEquals(setOf("firstHex", "secondHex", "weight"), snapshot.toInputs().keys)
        for (weight in listOf("0", "100")) assertNotNull(parseColorBlendSnapshot(valid + ("weight" to weight)))
    }
    @Test fun malformedOrExtendedSnapshotsAreRejected() {
        assertNull(parseColorBlendSnapshot(valid - "weight"))
        assertNull(parseColorBlendSnapshot(valid + ("output" to "#FFFFFF")))
        for (text in listOf("#fff", "AABBCC", "#FFAABBCC", "#GGGGGG", " #AABBCC", "#AABBCC\n")) {
            assertNull(parseColorBlendSnapshot(valid + ("firstHex" to text)))
            assertNull(parseColorBlendSnapshot(valid + ("secondHex" to text)))
        }
        for (weight in listOf("", "-1", "101", "1.5", "050", "+50", " 50", "50\n", "999999999999"))
            assertNull(parseColorBlendSnapshot(valid + ("weight" to weight)))
    }
    @Test fun modeRetentionAcceptsOnlyValidatedSetupAndRouteHasNormalConsentDefaults() {
        val route = "tool/color_blend"
        assertTrue(ToolMemoryPolicy.permitsRoute(route))
        assertTrue(ToolMemoryPolicy.validMode(route, valid))
        assertFalse(ToolMemoryPolicy.validMode(route, mapOf("text" to "unrelated")))
        assertFalse(ToolMemorySettings().permitsHistory(route))
        assertFalse(ToolMemorySettings().permitsResult(route))
        assertTrue(ToolMemorySettings(historyEnabled = true, rememberResults = true).permitsHistory(route))
        assertTrue(canRestoreToolSnapshot(route))
    }
}
