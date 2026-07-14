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
}
