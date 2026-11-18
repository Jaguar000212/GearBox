package com.jaguar.gearbox
import com.jaguar.gearbox.logic.*
import com.jaguar.gearbox.data.*
import org.junit.Assert.*
import org.junit.Test
class HtmlFormatterTest {
    @Test fun repairsAndWrapsFragmentsWithoutClaimingValidation() {
        val result=evaluateHtml("<p>Hello &amp; goodbye")
        assertNull(result.error)
        assertTrue(result.output!!.contains("<html>"))
        assertTrue(result.output.contains("<body><p>Hello &amp; goodbye</p></body>"))
        assertEquals(result.output,evaluateHtml(result.output).output)
        assertEquals(HtmlFormatErrorKind.UNSUPPORTED,evaluateHtml("<p>x",JsonFormatMode.VALIDATE).error)
        assertEquals(HtmlFormatErrorKind.UNSUPPORTED,evaluateHtml("<p>x",JsonFormatMode.MINIFY).error)
    }
    @Test fun preservesInertWhitespaceSensitiveContentButDoesNotSanitize() {
        val source="<html><head><style>p { color: red; }</style><script>if (a < b) { x++; }</script></head><body><!-- note --><pre> a\n  b</pre><textarea> x\n y</textarea><a href='javascript:alert(1)' onclick='run()'>go</a></body></html>"
        val output=evaluateHtml(source).output!!
        assertTrue(output.contains("<pre> a\n  b</pre>"))
        assertTrue(output.contains("<textarea> x\n y</textarea>"))
        assertTrue(output.contains("p { color: red; }"))
        assertTrue(output.contains("if (a < b) { x++; }"))
        assertTrue(output.contains("<!-- note -->"))
        assertTrue(output.contains("javascript:alert(1)"));assertTrue(output.contains("onclick=\"run()\""))
    }
    @Test fun complexityAndSchemaStayBounded() {
        assertEquals(HtmlFormatErrorKind.INPUT_LIMIT,evaluateHtml("x".repeat(64001)).error)
        assertEquals(HtmlFormatErrorKind.OUTPUT_LIMIT,evaluateHtml("&".repeat(64000)).error)
        assertEquals(HtmlFormatErrorKind.DEPTH_LIMIT,evaluateHtml("<div>".repeat(70)+"x"+"</div>".repeat(70)).error)
        assertEquals(HtmlFormatErrorKind.NODE_LIMIT,evaluateHtml("<p>".repeat(21000)).error)
    }
    @Test fun htmlSnapshotPreservesRawSourceAndRequiresNormalize() {
        val values=mapOf("source" to " <P a='b'> x ","mode" to "PRETTY","indent" to "2","language" to "HTML")
        assertEquals(values,parseJsonFormatterSnapshot(values)!!.toInputs())
        assertTrue(validJsonFormatterMode(values-"source"));assertFalse(validJsonFormatterMode(values))
        assertNull(parseJsonFormatterSnapshot(values+mapOf("mode" to "VALIDATE")))
        assertNull(parseJsonFormatterSnapshot(values+mapOf("mode" to "MINIFY")))
    }
}
