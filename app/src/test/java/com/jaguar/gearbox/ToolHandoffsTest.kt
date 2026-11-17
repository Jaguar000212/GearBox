package com.jaguar.gearbox

import com.jaguar.gearbox.data.compatibleToolHandoffs
import org.junit.Assert.*
import org.junit.Test

class ToolHandoffsTest {
    @Test fun textIsPassedExactlyAndNeverBackToSource() {
        val text = "  Hello\nworld  "
        val choices = compatibleToolHandoffs("tool/text_tools", mapOf("text" to "original"), text)
        assertFalse(choices.any { it.route == "tool/text_tools" })
        assertEquals(text, choices.single { it.route == "tool/qr_generator" }.inputs["text"])
        assertEquals("SHA256", choices.single { it.route == "tool/hash_generator" }.inputs["algorithm"])
    }
    @Test fun numericHandoffUsesTypedSourceRatherThanDisplayLabels() {
        val choices = compatibleToolHandoffs("tool/base_converter", mapOf("input" to "ff", "fromBase" to "HEXADECIMAL"), "Decimal: 255")
        assertEquals("255", choices.single { it.route == "tool/number_to_words" }.inputs["input"])
        assertFalse(choices.any { it.route == "tool/base_converter" })
        assertTrue(compatibleToolHandoffs("tool/unit_converter", mapOf("input" to "12"), "12 kg").isEmpty())
    }
    @Test fun romanBoundsAndInvalidRadixAreRespected() {
        for (value in listOf("0", "4000", "-1")) {
            assertFalse(compatibleToolHandoffs("tool/number_to_words", mapOf("input" to value), "words").any { it.route == "tool/number_to_roman" })
        }
        assertTrue(compatibleToolHandoffs("tool/base_converter", mapOf("input" to "99", "fromBase" to "BINARY"), "invalid").isEmpty())
    }
    @Test fun partialMetadataBlocksHandoffRegardlessOfTranslatedDisplay() {
        assertTrue(compatibleToolHandoffs("tool/morse_binary", mapOf("_partial" to "true"), "Traduction partielle: ...").isEmpty())
        assertFalse(compatibleToolHandoffs("tool/morse_binary", mapOf("_partial" to "false"), "SOS").isEmpty())
    }
    @Test fun secretsOversizeAndIncompleteTranslationsHaveNoHandoff() {
        assertTrue(compatibleToolHandoffs("tool/password_generator", emptyMap(), "secret").isEmpty())
        assertTrue(compatibleToolHandoffs("tool/text_tools", mapOf("text" to "x".repeat(9000)), "x").isEmpty())
        assertTrue(compatibleToolHandoffs("tool/morse_binary", emptyMap(), "Partial translation: ...").isEmpty())
        assertTrue(compatibleToolHandoffs("tool/text_tools", emptyMap(), "").isEmpty())
    }
    @Test fun wholeDecimalsUseExactInputsAndRespectDestinationRanges() {
        val choices = compatibleToolHandoffs("tool/decimal_to_fraction", mapOf("input" to "12.000"), "localized fraction")
        assertEquals("12", choices.single { it.route == "tool/base_converter" }.inputs["input"])
        assertTrue(compatibleToolHandoffs("tool/decimal_to_fraction", mapOf("input" to "12.5"), "25/2").isEmpty())
        assertFalse(compatibleToolHandoffs("tool/number_to_words", mapOf("input" to Long.MAX_VALUE.toString()), "words")
            .any { it.route == "tool/decimal_to_fraction" })
    }
    @Test fun ratioHandoffUsesTypedQuotientNeverFormattedOutput() {
        val choice = compatibleToolHandoffs("tool/ratios", mapOf("partA" to "1", "partB" to "4"), "unrelated localized output").single()
        assertEquals("tool/decimal_to_fraction", choice.route)
        assertEquals("0.25", choice.inputs["input"])
        assertEquals(com.jaguar.gearbox.data.HandoffMeaning.RATIO_QUOTIENT, choice.meaning)
        for (b in listOf("0", "NaN", "Infinity", "1e-300")) {
            assertTrue(compatibleToolHandoffs("tool/ratios", mapOf("partA" to "1", "partB" to b), "result").isEmpty())
        }
        for (route in listOf("tool/tip", "tool/unit_converter", "tool/random_number", "tool/percentage")) {
            assertTrue(compatibleToolHandoffs(route, mapOf("input" to "12"), "12").isEmpty())
        }
    }
}
