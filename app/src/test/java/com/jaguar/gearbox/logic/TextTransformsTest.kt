package com.jaguar.gearbox.logic

import org.junit.Assert.assertEquals
import org.junit.Test

/** Locks down existing transformations; Unicode behavior changes require a product decision. */
class TextTransformsTest {
    @Test fun existingRulesRemainStable() {
        assertEquals("Hello  World", convertCase("hello  world", TextCase.TITLE))
        assertEquals("Hello\tworld", convertCase("hello\tworld", TextCase.TITLE))
        assertEquals("helloWorld", convertCase("Hello-world", TextCase.CAMEL))
        assertEquals("caf", convertCase("café", TextCase.CAMEL))
        assertEquals("नमस्ते 🌍", convertCase("नमस्ते 🌍", TextCase.UPPER))
        assertEquals("", convertCase("", TextCase.TITLE))
    }
}
