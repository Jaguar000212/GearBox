package com.jaguar.gearbox

import com.jaguar.gearbox.data.*
import org.junit.Assert.*
import org.junit.Test

class ToolSearchEngineTest {
    private val index get() = ToolSearchCatalog.englishDocuments()
    private fun matches(query: String) = ToolSearchEngine.rank(query, index)

    @Test fun naturalTaskPhrasesNormalizePunctuationAndMatchExistingTools() {
        assertEquals(Tools.ROUTE_BUBBLE_LEVEL, matches("Is this straight?!").first().route)
        assertEquals(Tools.ROUTE_COMPASS, matches("which way is north?").first().route)
        assertEquals(Tools.ROUTE_AGE, matches("HOW OLD AM I?").first().route)
        assertTrue(matches("unsupported quantum teleporter").isEmpty())
        assertEquals("tool/focus_timer", matches("pomodoro interval").first().route)
        assertEquals("tool/aspect_ratio", matches("calculate image dimensions").first().route)
        assertNull(matches("aspect ratio").first().operationId)
        assertEquals("tool/decision_matrix", matches("compare options").first().route)
        assertNull(matches("decision matrix").first().operationId)
        assertEquals("tool/time_zone_converter", matches("convert time zones").first().route)
        assertNull(matches("time zone converter").first().operationId)
        assertEquals("tool/text_diff", matches("compare two texts").first().route)
        assertNull(matches("text diff").first().operationId)
        assertEquals("tool/scientific_calculator", matches("calculate square root").first().route)
        assertNull(matches("scientific calculator").first().operationId)
        assertEquals("tool/business_day_planner", matches("business days between dates").first().route)
        assertNull(matches("add business days").first().operationId)
        assertEquals("tool/bill_splitter", matches("split the bill").first().route)
        assertEquals("tool/bill_splitter", matches("who owes whom").first().route)
        assertNull(matches("shared expenses").first().operationId)
        assertNull(matches("split the bill").first().operationId)
        assertEquals("tool/color_blend", matches("color blend").first().route)
        assertEquals("tool/color_blend", matches("mix colours").first().route)
        assertEquals("tool/color_picker", matches("color hex rgb hsl").first().route)
        assertNull(matches("color blend").first().operationId)
    }

    @Test fun explicitOperationsResolveToSafeActualSelectors() {
        val circle = matches("circle area").first()
        assertEquals(Tools.ROUTE_GEOMETRY, circle.route)
        assertEquals("geometry.circle", circle.operationId)
        assertEquals(mapOf("shape" to "CIRCLE"), ToolSearchCatalog.operationInputs(circle.operationId!!)!!.second)
        val temperature = matches("temperature").first()
        assertEquals("units.temperature", temperature.operationId)
        assertEquals(mapOf("category" to "TEMPERATURE"), ToolSearchCatalog.operationInputs(temperature.operationId!!)!!.second)
        assertEquals("text.upper", matches("uppercase").first().operationId)
        assertEquals("hash.md5", matches("MD5").first().operationId)
        assertEquals("percentage.what_percent", matches("what percent").first().operationId)
        assertEquals("percentage.change", matches("percent increase").first().operationId)
        assertEquals("morse.text_to_binary", matches("text to binary").first().operationId)
        assertEquals("morse.binary_to_text", matches("binary to text").first().operationId)
        assertEquals("morse.binary_to_text", matches("decode binary").first().operationId)
        assertEquals("text.lower", matches("make text lowercase").first().operationId)
    }

    @Test fun broadOrTiedQueryDoesNotChooseArbitraryModeAndToolsNeverDuplicate() {
        assertNull(matches("area").first { it.route == Tools.ROUTE_GEOMETRY }.operationId)
        assertNull(matches("Unit Converter").first().operationId)
        for (query in listOf("area", "convert", "text", "hash", "")) {
            val rows = matches(query)
            assertEquals(rows.size, rows.map { it.route }.distinct().size)
        }
        assertTrue(matches("").all { it.operationId == null })
        assertEquals(Tools.all.map { it.route }, matches("").map { it.route })
    }

    @Test fun matchingRequiresAllMeaningfulWordsAndSupportsTypingPrefixes() {
        assertTrue(matches("circle impossibleword").isEmpty())
        assertEquals("units.temperature", matches("tempera").first().operationId)
        assertEquals("geometry.circle", matches("geometry circle area").first().operationId)
        assertEquals("café", ToolSearchEngine.normalize("CAFÉ?!"))
    }

    @Test fun operationInputsContainOnlyKnownConfigurationKeysAndNeverUserContent() {
        val allowed = setOf("shape", "category", "mode", "case", "fromBase", "algorithm")
        index.flatMap { it.operations }.forEach { op ->
            val (route, values) = ToolSearchCatalog.operationInputs(op.id)!!
            assertTrue(Tools.all.any { it.route == route })
            assertTrue(values.isNotEmpty() && values.keys.all { it in allowed })
            assertTrue(ToolMemoryPolicy.validInputs(values))
        }
        assertNull(ToolSearchCatalog.operationInputs("not-a-real-operation"))
    }
}
