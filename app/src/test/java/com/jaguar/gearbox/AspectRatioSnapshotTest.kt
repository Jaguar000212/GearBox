package com.jaguar.gearbox

import com.jaguar.gearbox.data.*
import org.junit.Assert.*
import org.junit.Test

class AspectRatioSnapshotTest {
    @Test fun onlyActiveFieldsAreCapturedAndRatioNeedsOnlySource() {
        val inputs = aspectRatioInputs("WIDTH", "1920", "1080", "1280", "invalid hidden draft", "private unused")
        assertEquals("", inputs["targetHeight"])
        assertEquals("", inputs["percent"])
        assertEquals(inputs, parseAspectRatioSnapshot(inputs)!!.toInputs())
        assertNotNull(parseAspectRatioSnapshot(aspectRatioInputs("RATIO", "1920", "1080", "", "", "")))
    }
    @Test fun percentSpellingAndModeRelevantInputsRemainExact() {
        val inputs = aspectRatioInputs("PERCENT", "100", "100", "", "", "50.00")
        assertEquals(inputs, parseAspectRatioSnapshot(inputs)!!.toInputs())
        assertNotNull(parseAspectRatioSnapshot(aspectRatioInputs("HEIGHT", "100", "100", "", "50", "")))
        for (mode in listOf("FIT", "FILL")) {
            assertNotNull(parseAspectRatioSnapshot(aspectRatioInputs(mode, "100", "50", "200", "200", "")))
            assertNull(parseAspectRatioSnapshot(aspectRatioInputs(mode, "100", "50", "200", "", "")))
        }
    }
    @Test fun strictSchemaRejectsInactiveContentInvalidDimensionsAndPercent() {
        val valid = aspectRatioInputs("WIDTH", "1920", "1080", "1280", "", "")
        assertNull(parseAspectRatioSnapshot(valid + ("targetHeight" to "100")))
        assertNull(parseAspectRatioSnapshot(valid + ("file" to "not supported")))
        assertNull(parseAspectRatioSnapshot(valid - "percent"))
        for (value in listOf("0", "-1", "1.5", "01", "1000001", " 1"))
            assertNull(parseAspectRatioSnapshot(valid + ("sourceWidth" to value)))
        for (value in listOf("0", "0.001", "10000.01", "NaN", "1e2"))
            assertNull(parseAspectRatioSnapshot(aspectRatioInputs("PERCENT", "100", "100", "", "", value)))
    }
    @Test fun modeRetentionHasNoDimensionsAndResultCaptureIsOptIn() {
        val route = "tool/aspect_ratio"
        assertTrue(ToolMemoryPolicy.validMode(route, mapOf("mode" to "FIT")))
        assertTrue(ToolMemoryPolicy.validMode(route, mapOf("mode" to "RATIO")))
        assertFalse(ToolMemoryPolicy.validMode(route, aspectRatioInputs("WIDTH", "100", "100", "50", "", "")))
        assertFalse(ToolMemoryPolicy.validMode(route, mapOf("mode" to "unknown")))
        assertFalse(ToolMemorySettings().permitsResult(route))
        assertTrue(ToolMemorySettings(rememberResults = true).permitsResult(route))
    }
}
