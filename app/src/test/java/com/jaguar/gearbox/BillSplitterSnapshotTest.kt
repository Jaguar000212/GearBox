package com.jaguar.gearbox

import com.jaguar.gearbox.data.*
import org.junit.Assert.*
import org.junit.Test

class BillSplitterSnapshotTest {
    private val valid = mapOf("bill" to "100.00", "tax" to "0.0", "tipPercent" to "10.00", "diners" to "3")
    @Test fun validatedSnapshotPreservesDecimalSpellingForHydration() {
        val parsed = parseBillSplitterSnapshot(valid)!!
        assertEquals(valid, parsed.toInputs())
        assertEquals(3, parsed.diners)
        assertNotNull(parseBillSplitterSnapshot(mapOf("bill" to "1000000000.00", "tax" to "1000000000", "tipPercent" to "100", "diners" to "1000")))
    }
    @Test fun rejectsWrongSchemasAndUnboundedOrNoncanonicalInputs() {
        assertNull(parseBillSplitterSnapshot(valid - "tax"))
        assertNull(parseBillSplitterSnapshot(valid + ("currencyLabel" to "USD")))
        assertNull(parseBillSplitterSnapshot(valid + ("names" to "Alice")))
        for (value in listOf("-1", "1e3", "NaN", "1.001", " 1", "01", "1000000000.01")) {
            assertNull(parseBillSplitterSnapshot(valid + ("bill" to value)))
            assertNull(parseBillSplitterSnapshot(valid + ("tax" to value)))
        }
        for (value in listOf("-1", "100.01", "1.001", "1e1"))
            assertNull(parseBillSplitterSnapshot(valid + ("tipPercent" to value)))
        for (value in listOf("0", "1001", "03", "3.0", "+3", " 3"))
            assertNull(parseBillSplitterSnapshot(valid + ("diners" to value)))
    }
    @Test fun modeRetentionIsDinersOnlyAndNormalCaptureRequiresGlobalConsent() {
        val route = "tool/bill_splitter"
        assertTrue(ToolMemoryPolicy.validMode(route, mapOf("diners" to "3")))
        assertFalse(ToolMemoryPolicy.validMode(route, valid))
        assertFalse(ToolMemoryPolicy.validMode(route, mapOf("diners" to "0")))
        assertFalse(ToolMemorySettings().permitsHistory(route))
        assertFalse(ToolMemorySettings().permitsResult(route))
        assertTrue(ToolMemorySettings(historyEnabled = true, rememberResults = true).permitsResult(route))
    }
}
