package com.jaguar.gearbox.logic

import org.junit.Assert.*
import org.junit.Test

class NumericInputTest {
    @Test fun `blank and finite input are neutral`() {
        listOf("", "  ", "-1.25", "0", " 3.5 ", "1e6").forEach {
            assertFalse(it, isInvalidNumericInput(it))
        }
    }
    @Test fun `partial invalid and nonfinite input are rejected`() {
        listOf("-", ".", "text", "NaN", "Infinity", "1e999").forEach {
            assertTrue(it, isInvalidNumericInput(it))
        }
    }
}
