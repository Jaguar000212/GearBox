package com.jaguar.gearbox.logic

import org.junit.Assert.*
import org.junit.Test

class PasswordsTest {
    @Test fun `all required classes appear and length is exact`() {
        val classes = listOf("ABC", "xyz", "012", "!?_")
        repeat(100) {
            val value = generatePassword(16, classes)
            assertEquals(16, value.length)
            classes.forEach { chars -> assertTrue(value.any { it in chars }) }
            assertTrue(value.all { it in classes.joinToString("") })
        }
    }

    @Test fun `minimum length still represents every class`() {
        assertEquals(setOf('A', 'b', '3', '!'), generatePassword(4, listOf("A", "b", "3", "!")).toSet())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `too short for selected classes is rejected`() {
        generatePassword(1, listOf("a", "b"))
    }

    @Test fun `invalid saved settings do not produce a strong rating`() {
        assertEquals("Unavailable" to 0f, passwordStrength(16, 0))
        assertEquals("Unavailable" to 0f, passwordStrength(16, 1))
        assertEquals("Unavailable" to 0f, passwordStrength(0, 62))
    }

    @Test fun `rating uses generation snapshot`() {
        val generatedLength = 16
        val generatedCharsetSize = 10
        val rating = passwordStrength(generatedLength, generatedCharsetSize)
        assertEquals("Medium" to 0.5f, rating)
        // Changing the form to a larger alphabet must not change the saved snapshot.
        assertNotEquals(rating, passwordStrength(generatedLength, 62))
        assertEquals(rating, passwordStrength(generatedLength, generatedCharsetSize))
    }
}
