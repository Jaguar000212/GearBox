package com.jaguar.gearbox.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MorseCodeTest {

    @Test
    fun `text to morse encodes words separated by slash`() {
        assertEquals("... --- ...", textToMorse("SOS"))
        assertEquals(".... .. / - .... . .-. .", textToMorse("HI THERE"))
    }

    @Test
    fun `morse to text round-trips`() {
        assertEquals("SOS", morseToText(textToMorse("SOS")))
        assertEquals("HI THERE", morseToText(textToMorse("HI THERE")))
    }

    @Test
    fun `text to binary and back round-trips`() {
        val binary = textToBinary("Hi")
        assertEquals("01001000 01101001", binary)
        assertEquals("Hi", binaryToText(binary))
    }

    @Test
    fun `invalid binary tokens return null`() {
        assertNull(binaryToText("not binary"))
        assertNull(binaryToText("012"))
    }

    @Test
    fun `unicode and emoji round trip as UTF8`() {
        listOf("café", "नमस्ते", "你好", "😀", "A\nB\tC", "a\u0000b").forEach {
            assertEquals(it, binaryToText(textToBinary(it)))
        }
        assertEquals("11000011 10101001", textToBinary("é"))
    }

    @Test
    fun `malformed UTF8 is rejected`() {
        listOf("10000000", "11000011", "11000000 10101111", "11101101 10100000 10000000", "11110100 10010000 10000000 10000000", "111111111").forEach {
            assertNull(binaryToText(it))
        }
        assertEquals("A", binaryToText("1000001"))
        assertEquals("Hi", binaryToText(" 01001000\n01101001 "))
    }

    @Test
    fun `Morse warnings expose omissions without changing output`() {
        org.junit.Assert.assertTrue(hasUnsupportedMorseText("HI 😀"))
        org.junit.Assert.assertFalse(hasUnsupportedMorseText("Hi!"))
        org.junit.Assert.assertTrue(hasUnknownMorseTokens("... invalid"))
        org.junit.Assert.assertFalse(hasUnknownMorseTokens(".... .. / - .... . .-. ."))
        assertEquals("S", morseToText("... invalid"))
    }
}
