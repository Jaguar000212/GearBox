package com.jaguar.gearbox.logic

import org.junit.Assert.*
import org.junit.Test

class TextHashesTest {
    @Test fun knownVectors() {
        assertEquals("900150983cd24fb0d6963f7d28e17f72", textHashHex("abc", TextHashAlgorithm.MD5))
        assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d", textHashHex("abc", TextHashAlgorithm.SHA1))
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", textHashHex("abc", TextHashAlgorithm.SHA256))
        assertEquals(128, textHashHex("abc", TextHashAlgorithm.SHA512).length)
    }
    @Test fun preservesExactInputAndLeadingZeroes() {
        assertNotEquals(textHashHex("hi", TextHashAlgorithm.SHA256), textHashHex("hi\n", TextHashAlgorithm.SHA256))
        assertEquals("0cc175b9c0f1b6a831c399e269772661", textHashHex("a", TextHashAlgorithm.MD5))
        assertEquals(64, textHashHex("नमस्ते 🌍", TextHashAlgorithm.SHA256).length)
    }
}
