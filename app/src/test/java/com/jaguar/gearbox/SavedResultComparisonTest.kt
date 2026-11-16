package com.jaguar.gearbox

import com.jaguar.gearbox.data.*
import org.junit.Assert.*
import org.junit.Test

class SavedResultComparisonTest {
    private fun item(id: String, route: String = "tool/percentage", inputs: Map<String, String> = mapOf("mode" to "OF_VALUE", "field1" to "10", "field2" to "100")) =
        SavedToolItem(id, id, route, 123L, inputs, "stored result")
    @Test fun sameModeDifferentNumbersAreCompatibleWithoutRecomputation() {
        val first = item("a")
        val second = item("b", inputs = first.inputs + ("field1" to "20"))
        assertTrue(savedResultComparison(first, second).compatible)
        assertEquals("stored result", second.output)
        assertEquals(SavedResultComparisonReason.SAME_ITEM, savedResultComparison(first, first).reason)
    }
    @Test fun missingDifferentAndExtraModesFailClosed() {
        val first = item("a")
        assertEquals(SavedResultComparisonReason.DIFFERENT_CONFIGURATION,
            savedResultComparison(first, item("b", inputs = first.inputs + ("mode" to "CHANGE"))).reason)
        assertFalse(savedResultComparison(first, item("b", inputs = first.inputs - "mode")).compatible)
        assertFalse(savedResultComparison(first, item("b", inputs = first.inputs + ("unknown" to "x"))).compatible)
    }
    @Test fun unitTargetsAndSourceCategoriesMustMatch() {
        val fields = mapOf("category" to "LENGTH", "fromUnit" to "Meters", "toUnit" to "Centimeters", "input" to "1")
        val first = item("a", "tool/unit_converter", fields)
        assertTrue(savedResultComparison(first, item("b", first.route, fields + ("input" to "2"))).compatible)
        for (key in listOf("category", "fromUnit", "toUnit"))
            assertFalse(savedResultComparison(first, item("b", first.route, fields + (key to "other"))).compatible)
    }
    @Test fun excludedSensitiveLiveGamesAndUnknownToolsRemainExcluded() {
        for (route in listOf("password_generator", "text_tools", "qr_generator", "hash_generator", "morse_binary", "bmi", "date_calculator", "counter", "timer", "dice", "compass", "new_tool")) {
            assertFalse(route, savedResultComparison(item("a", "tool/$route"), item("b", "tool/$route")).compatible)
        }
        assertFalse(savedResultComparison(item("a"), item("b", "tool/geometry")).compatible)
        assertFalse(savedResultComparison(item("a"), item("b").copy(output = "")).compatible)
        assertFalse(savedResultComparison(item("a"), item("b").copy(output = "x".repeat(8193))).compatible)
    }
    @Test fun identicalUnknownConfigurationsAreNotCompatible() {
        val bad = listOf(
            item("a", inputs = mapOf("mode" to "unknown", "field1" to "10", "field2" to "100")),
            item("a", "tool/geometry", mapOf("shape" to "unknown", "field1" to "1", "field2" to "2", "field3" to "")),
            item("a", "tool/gst", mapOf("inclusive" to "TRUE", "amount" to "1", "rate" to "2")),
            item("a", "tool/base_converter", mapOf("fromBase" to "BASE36", "input" to "1")),
            item("a", "tool/number_to_words", mapOf("system" to "unknown", "input" to "1")),
            item("a", "tool/unit_converter", mapOf("category" to "LENGTH", "fromUnit" to "Meters", "toUnit" to "Grams", "input" to "1")),
            item("a", "tool/unit_converter", mapOf("category" to "UNKNOWN", "fromUnit" to "Meters", "toUnit" to "Meters", "input" to "1")),
        )
        bad.forEach { assertEquals(it.route, SavedResultComparisonReason.INVALID_SNAPSHOT,
            savedResultComparison(it, it.copy(id = "b")).reason) }
    }

    @Test fun numericTipScenarioParametersCanDiffer() {
        val first = item("a", "tool/tip", mapOf("bill" to "100", "tipPercent" to "10", "splitCount" to "2"))
        val second = item("b", first.route, mapOf("bill" to "200", "tipPercent" to "20", "splitCount" to "4"))
        assertTrue(savedResultComparison(first, second).compatible)
    }
}
