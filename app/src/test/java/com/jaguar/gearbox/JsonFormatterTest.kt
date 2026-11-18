package com.jaguar.gearbox

import com.jaguar.gearbox.logic.*
import org.junit.Assert.*
import org.junit.Test

class JsonFormatterTest {
    private fun minify(source: String) = evaluateJson(source, JsonFormatMode.MINIFY)
    @Test fun allTopLevelValuesAndExactNumberSpelling() {
        for (value in listOf("null", "true", "false", "123456789012345678901234567890", "-0.00E+9000", "\"text\"", "[]", "{}"))
            assertEquals(value, minify(" $value \n").output)
        assertEquals("[1.00,1e+03,-0,1E999999]", minify("[ 1.00, 1e+03, -0, 1E999999 ]").output)
    }
    @Test fun stringsEscapesAndKeyOrderArePreserved() {
        val source = "{ \"z\" : \"\\u0061\\n\\/\\\\\\\"\", \"a\":\"\\uD83D\\uDE00\" }"
        val min = minify(source)
        assertNull(min.error)
        val pretty = evaluateJson(source, JsonFormatMode.PRETTY, 4)
        assertTrue(pretty.output!!.contains("\n    \"z\": "))
        assertEquals(min.output, minify(pretty.output!!).output)
    }
    @Test fun invalidGrammarAndInteroperabilityAreRejected() {
        for (source in listOf("", "01", "+1", ".5", "1.", "1e", "NaN", "Infinity", "[1,]", "{\"a\":1,}", "// hi\n1", "{a:1}", "true false", "\"\\x00\"", "\"raw\nnewline\""))
            assertEquals(source, JsonFormatErrorKind.SYNTAX, minify(source).error?.kind)
        assertEquals(JsonFormatErrorKind.DUPLICATE_KEY, minify("{\"a\":1,\"\\u0061\":2}").error?.kind)
        assertEquals(JsonFormatErrorKind.INVALID_UNICODE, minify("\"\\uD800\"").error?.kind)
        assertEquals(JsonFormatErrorKind.INVALID_UNICODE, minify("\"\\uDC00\"").error?.kind)
    }
    @Test fun locationsAreUtf16BasedAndCrLfCountsOnce() {
        val error = minify("{\r\n  \"a\": ]}").error!!
        assertEquals(2, error.line); assertEquals(8, error.column)
        val unicode = minify("\"a\\uD800\"").error!!
        assertEquals(3, unicode.column)
    }
    @Test fun depthInputTokensAndOutputAreBounded() {
        assertNull(minify("[".repeat(64) + "0" + "]".repeat(64)).error)
        assertEquals(JsonFormatErrorKind.DEPTH_LIMIT, minify("[".repeat(65) + "0" + "]".repeat(65)).error?.kind)
        assertEquals(JsonFormatErrorKind.INPUT_LIMIT, minify(" ".repeat(64001)).error?.kind)
        assertEquals(JsonFormatErrorKind.TOKEN_LIMIT, minify("[" + List(10001) { "0" }.joinToString(",") + "]").error?.kind)
        val expanded = "[".repeat(63) + "[" + List(2000) { "0" }.joinToString(",") + "]" + "]".repeat(63)
        assertEquals(JsonFormatErrorKind.OUTPUT_LIMIT, evaluateJson(expanded, JsonFormatMode.PRETTY,4).error?.kind)
        assertNotNull(minify(expanded).output)
    }
    @Test fun validationDoesNotFormatOrExposePartialOutput() {
        assertEquals("", evaluateJson("{\"x\":1}", JsonFormatMode.VALIDATE).output)
        assertNull(minify("[1,").output)
    }
}
