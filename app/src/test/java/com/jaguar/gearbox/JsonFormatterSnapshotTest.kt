package com.jaguar.gearbox

import com.jaguar.gearbox.data.*
import org.junit.Assert.*
import org.junit.Test

class JsonFormatterSnapshotTest {
    private val inputs = mapOf("source" to " {\r\n\"n\":1.00e+2,\"s\":\"\\u0061\"} ", "mode" to "PRETTY", "indent" to "2")
    private val route = "tool/json_formatter"

    @Test fun rawLexicalSourceAndInvalidDraftsRoundTripWithoutParsing() {
        assertEquals(inputs, parseJsonFormatterSnapshot(inputs)!!.toInputs())
        listOf("", "{broken", "{\"a\":1,\"a\":2}").forEach { source ->
            val draft = inputs + ("source" to source)
            assertEquals(draft, parseJsonFormatterSnapshot(draft)!!.toInputs())
        }
    }

    @Test fun optionalLanguagePreservesLegacyAndExplicitShape() {
        assertEquals(inputs,parseJsonFormatterSnapshot(inputs)!!.toInputs())
        for(language in listOf("JSON","XML")) {
            val values=inputs+mapOf("language" to language)
            assertEquals(values,parseJsonFormatterSnapshot(values)!!.toInputs())
            assertTrue(validJsonFormatterMode(values-"source"))
            assertFalse(validJsonFormatterMode(values))
        }
        assertNull(parseJsonFormatterSnapshot(inputs+mapOf("language" to "XML","mode" to "MINIFY")))
        assertNull(parseJsonFormatterSnapshot(inputs+mapOf("language" to "SQL")))
    }

    @Test fun exactEnvelopeModesAndBoundsFailClosed() {
        assertNull(parseJsonFormatterSnapshot(inputs - "source"))
        assertNull(parseJsonFormatterSnapshot(inputs + ("extra" to "value")))
        assertNull(parseJsonFormatterSnapshot(inputs + ("mode" to "pretty")))
        listOf("02", "3", "+2", " 4").forEach { assertNull(parseJsonFormatterSnapshot(inputs + ("indent" to it))) }
        assertNotNull(parseJsonFormatterSnapshot(inputs + ("source" to "x".repeat(64000))))
        assertNull(parseJsonFormatterSnapshot(inputs + ("source" to "x".repeat(64001))))
        listOf("VALIDATE", "PRETTY", "MINIFY").forEach { mode ->
            assertNotNull(parseJsonFormatterSnapshot(inputs + mapOf("mode" to mode, "indent" to "4")))
        }
    }

    @Test fun editorBoundsDoNotExpandPersistenceOrTruncateContent() {
        val long = inputs + ("source" to "x".repeat(4097))
        assertNotNull(parseJsonFormatterSnapshot(long))
        assertFalse(ToolMemoryPolicy.validRecord(long, "output"))
        assertFalse(ToolMemoryPolicy.validRecord(inputs, "x".repeat(4097)))
        assertTrue(ToolMemoryPolicy.validRecord(inputs, "output"))
    }

    @Test fun contentNeverBecomesAModeAndAutomaticCaptureNeedsBothConsents() {
        assertTrue(ToolMemoryPolicy.isSensitiveTextRoute(route))
        val global = ToolMemorySettings(historyEnabled = true, rememberResults = true)
        assertFalse(global.permitsHistory(route))
        assertFalse(global.permitsResult(route))
        val specific = mapOf(route to ToolAutomaticMemory(true, true))
        assertFalse(ToolMemorySettings(automaticMemory = specific).permitsHistory(route))
        assertFalse(ToolMemorySettings(automaticMemory = specific).permitsResult(route))
        assertTrue(global.copy(automaticMemory = specific).permitsHistory(route))
        assertTrue(global.copy(automaticMemory = specific).permitsResult(route))
        assertTrue(ToolMemoryPolicy.validMode(route, inputs - "source"))
        assertFalse(ToolMemoryPolicy.validMode(route, inputs))
        assertFalse(ToolMemoryPolicy.validMode(route, mapOf("source" to "private")))
    }
}
