package com.jaguar.gearbox

import com.jaguar.gearbox.data.*
import org.junit.Assert.*
import org.junit.Test

class DecisionMatrixSnapshotTest {
    private val valid = mapOf("optionLabels" to "Option A\nOption B", "criterionLabels" to "Value\nComfort",
        "weights" to "1,1", "scores" to "4,3\n3,4")
    @Test fun compactRepresentationRoundTripsPreservingLabelWhitespace() {
        val values = valid + ("optionLabels" to " Option A \nOption B")
        assertEquals(values, parseDecisionMatrixSnapshot(values)!!.toInputs())
        assertEquals(listOf(listOf(4, 3), listOf(3, 4)), parseDecisionMatrixSnapshot(valid)!!.scores)
    }
    @Test fun maximumMatrixFitsUnchangedStorageCaps() {
        val maximum = mapOf("optionLabels" to List(6) { "A".repeat(60) }.joinToString("\n"),
            "criterionLabels" to List(6) { "B".repeat(60) }.joinToString("\n"),
            "weights" to List(6) { "100" }.joinToString(","),
            "scores" to List(6) { List(6) { "5" }.joinToString(",") }.joinToString("\n"))
        assertNotNull(parseDecisionMatrixSnapshot(maximum))
        assertTrue(ToolMemoryPolicy.validRecord(maximum, "ranked result"))
    }
    @Test fun rejectsUnsupportedFieldsMalformedDimensionsAndNoncanonicalValues() {
        assertNull(parseDecisionMatrixSnapshot(valid + ("names" to "private")))
        assertNull(parseDecisionMatrixSnapshot(valid - "weights"))
        for ((key, value) in listOf("optionLabels" to "Only one", "optionLabels" to List(7) { "A" }.joinToString("\n"),
            "criterionLabels" to "", "optionLabels" to "A\tB\nC", "optionLabels" to "A".repeat(61) + "\nB",
            "weights" to "0,0", "weights" to "101,1", "weights" to "01,1", "weights" to "1",
            "scores" to "4,3", "scores" to "4,3,2\n3,4", "scores" to "6,3\n3,4", "scores" to "4.0,3\n3,4"))
            assertNull("$key=$value", parseDecisionMatrixSnapshot(valid + (key to value)))
    }
    @Test fun unicodeLineAndParagraphSeparatorsAreRejectedInBothLabelLists() {
        for (separator in listOf('\u2028', '\u2029')) {
            assertNull(parseDecisionMatrixSnapshot(valid + ("optionLabels" to "A${separator}B\nC")))
            assertNull(parseDecisionMatrixSnapshot(valid + ("criterionLabels" to "A${separator}B\nC")))
        }
    }

    @Test fun privateLabelsRequirePerToolConsentAndNeverEnterModeMemory() {
        val route = "tool/decision_matrix"
        val global = ToolMemorySettings(historyEnabled = true, rememberResults = true)
        assertTrue(ToolMemoryPolicy.isSensitiveTextRoute(route))
        assertFalse(global.permitsHistory(route))
        assertFalse(global.permitsResult(route))
        val allowed = global.copy(automaticMemory = mapOf(route to ToolAutomaticMemory(true, true)))
        assertTrue(allowed.permitsHistory(route))
        assertTrue(allowed.permitsResult(route))
        assertFalse(ToolMemoryPolicy.validMode(route, valid))
        assertTrue(ToolMemoryPolicy.validMode(route, emptyMap()))
    }
}
