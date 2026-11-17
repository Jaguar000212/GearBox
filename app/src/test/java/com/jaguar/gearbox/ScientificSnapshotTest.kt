package com.jaguar.gearbox

import com.jaguar.gearbox.data.*
import com.jaguar.gearbox.logic.ScientificAngleMode
import com.jaguar.gearbox.logic.evaluateScientific
import org.junit.Assert.*
import org.junit.Test

class ScientificSnapshotTest {
    @Test fun rawExpressionIsPreservedForHydrationWithoutNormalization() {
        val inputs = mapOf("expression" to "  sqrt(16) + 2^3  ", "angleMode" to "DEG")
        assertEquals(inputs, parseScientificSnapshot(inputs)!!.toInputs())
        assertNotNull(parseScientificSnapshot(inputs + ("angleMode" to "RAD")))
    }
    @Test fun unsupportedSchemasBlankOversizeAndUnknownModesAreRejected() {
        val inputs = mapOf("expression" to "1+2", "angleMode" to "DEG")
        assertNull(parseScientificSnapshot(inputs - "angleMode"))
        assertNull(parseScientificSnapshot(inputs + ("result" to "3")))
        assertNull(parseScientificSnapshot(inputs + ("angleMode" to "deg")))
        assertNull(parseScientificSnapshot(inputs + ("expression" to "   ")))
        assertNull(parseScientificSnapshot(inputs + ("expression" to "1".repeat(513))))
        assertNotNull(parseScientificSnapshot(inputs + ("expression" to "1".repeat(512))))
    }
    @Test fun invalidArithmeticIsOnlyInertInputNotAnAutomaticallyRestoredResult() {
        val snapshot = parseScientificSnapshot(mapOf("expression" to "1/0", "angleMode" to "DEG"))!!
        val result = evaluateScientific(snapshot.expression, ScientificAngleMode.DEG)
        assertNull(result.value)
        assertNotNull(result.error)
    }
    @Test fun modeRememberingCannotContainExpressionsAndCaptureIsOptIn() {
        val route = "tool/scientific_calculator"
        assertTrue(ToolMemoryPolicy.validMode(route, mapOf("angleMode" to "RAD")))
        assertFalse(ToolMemoryPolicy.validMode(route, mapOf("angleMode" to "RAD", "expression" to "1+2")))
        assertFalse(ToolMemoryPolicy.validMode(route, mapOf("angleMode" to "GRAD")))
        assertFalse(ToolMemorySettings().permitsHistory(route))
        assertFalse(ToolMemorySettings().permitsResult(route))
        assertTrue(ToolMemorySettings(historyEnabled = true, rememberResults = true).permitsResult(route))
    }
}
