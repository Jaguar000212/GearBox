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
    @Test fun secretsOversizeAndIncompleteTranslationsHaveNoHandoff() {
        assertTrue(compatibleToolHandoffs("tool/password_generator", emptyMap(), "secret").isEmpty())
        assertTrue(compatibleToolHandoffs("tool/text_tools", mapOf("text" to "x".repeat(9000)), "x").isEmpty())
        assertTrue(compatibleToolHandoffs("tool/morse_binary", emptyMap(), "Partial translation: ...").isEmpty())
        assertTrue(compatibleToolHandoffs("tool/text_tools", emptyMap(), "").isEmpty())
    }
}
