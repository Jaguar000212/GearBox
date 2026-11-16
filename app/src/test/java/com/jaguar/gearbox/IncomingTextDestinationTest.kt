package com.jaguar.gearbox

import com.jaguar.gearbox.data.*
import org.junit.Assert.*
import org.junit.Test

class IncomingTextDestinationTest {
    @Test fun literalPayloadsArePreservedAndModesAreExplicit() {
        val text = "  https://example.test/?q=hello\nworld  "
        for (destination in IncomingTextDestination.entries) {
            val inputs = requireNotNull(destination.inputs(text))
            assertEquals(text, inputs[if (destination == IncomingTextDestination.MORSE) "input" else "text"])
            assertTrue(ToolMemoryPolicy.validInputs(inputs))
        }
        assertEquals("UPPER", IncomingTextDestination.TEXT.inputs(text)?.get("case"))
        assertEquals("SHA256", IncomingTextDestination.HASH.inputs(text)?.get("algorithm"))
        assertEquals("TEXT_TO_MORSE", IncomingTextDestination.MORSE.inputs(text)?.get("mode"))
        assertEquals("true", IncomingTextDestination.MORSE.inputs("🙂")?.get("_partial"))
        assertEquals("false", IncomingTextDestination.MORSE.inputs("SOS")?.get("_partial"))
    }
    @Test fun boundsRejectRatherThanTruncate() {
        for (destination in IncomingTextDestination.entries) {
            assertNull(destination.inputs(" \n\t"))
            assertNull(destination.inputs("x".repeat(4097)))
            assertNotNull(destination.inputs("x".repeat(4096)))
        }
    }
}
