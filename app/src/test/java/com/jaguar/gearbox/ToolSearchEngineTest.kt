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
        assertTrue(matches("color blend").isEmpty())
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
