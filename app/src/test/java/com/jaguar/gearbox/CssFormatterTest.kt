package com.jaguar.gearbox
import com.jaguar.gearbox.logic.*
import com.jaguar.gearbox.data.*
import org.junit.Assert.*
import org.junit.Test
class CssFormatterTest {
    @Test fun separatesOnlyTopLevelBoundariesAndIsIdempotent() {
        val source="@charset \"UTF-8\";a{color:red} b{margin:0}"
        val output="@charset \"UTF-8\";\na{color:red}\n b{margin:0}"
        assertEquals(output,evaluateCss(source).output)
        assertEquals(output,evaluateCss(output).output)
    }
    @Test fun sensitiveInteriorsAndEscapesRemainExact() {
        val first="@media screen{a:hover{--x: { a:b; c:d };width:calc(100% - 2px);background:url('https://example.com/a;b}');content:'a\\\";{}';}}"
        val second=".a\\;b{--gap:  1px  2px ;content:\"x\\\n y\"}"
        val source=first+"/* comment ;{} */"+second
        assertEquals(first+"\n/* comment ;{} */"+second,evaluateCss(source).output)
        val hex=".\\31 23{color:red}x{color:blue}"
        assertEquals(".\\31 23{color:red}\nx{color:blue}",evaluateCss(hex).output)
    }
    @Test fun unquotedUrlsAndEscapedBracesAreConservative() {
        val source="a{background:url(data:image/png;base64,AAAA==)}b{color:red}"
        assertEquals("a{background:url(data:image/png;base64,AAAA==)}\nb{color:red}", evaluateCss(source).output)
        assertEquals(".a\\{b{color:red}\nx{}", evaluateCss(".a\\{b{color:red}x{}").output)
        // Layout scanner deliberately rejects an unmatched delimiter even inside an unquoted URL.
        assertEquals(CssFormatErrorKind.UNSUPPORTED, evaluateCss("a{background:url(data:x})}").error)
    }
    @Test fun malformedAndUnsupportedFailWithoutRepair() {
        for(source in listOf("a{", "a{]}","a{content:'x}","/* unfinished", "a{color:red}\\", "a{content:'a\nb'}", "color:red;", "@import url(x)"))
            assertEquals(source,CssFormatErrorKind.UNSUPPORTED,evaluateCss(source).error)
        assertEquals(CssFormatErrorKind.UNSUPPORTED,evaluateCss("a{}",JsonFormatMode.MINIFY).error)
        assertEquals(CssFormatErrorKind.UNSUPPORTED,evaluateCss("a{}",JsonFormatMode.VALIDATE).error)
    }
    @Test fun scalarValidationIncludesOpaqueBranchesAndLimitsStayBounded() {
        for(source in listOf("a{content:'\u0000'}", "/*\uD800*/a{}", "a{content:'\\\uDC00'}", "a{--x:\uDC00}"))
            assertEquals(CssFormatErrorKind.UNSUPPORTED,evaluateCss(source).error)
        assertEquals("a{content:'😀'}",evaluateCss("a{content:'😀'}").output)
        assertEquals(CssFormatErrorKind.INPUT_LIMIT,evaluateCss(" ".repeat(64001)).error)
        assertEquals(CssFormatErrorKind.DEPTH_LIMIT,evaluateCss("a{"+"(".repeat(64)+")".repeat(64)+"}").error)
        assertEquals(CssFormatErrorKind.TOKEN_LIMIT,evaluateCss(" ".repeat(20001)).error)
    }
    @Test fun schemaIsExplicitAndOnlyFormattingIsAllowed() {
        val values=mapOf("source" to "a{--x:  a b }","language" to "CSS","mode" to "PRETTY","indent" to "2")
        assertEquals(values,parseJsonFormatterSnapshot(values)!!.toInputs())
        assertTrue(validJsonFormatterMode(values-"source"));assertFalse(validJsonFormatterMode(values))
        assertNull(parseJsonFormatterSnapshot(values+mapOf("mode" to "VALIDATE")))
        assertNull(parseJsonFormatterSnapshot(values+mapOf("mode" to "MINIFY")))
        for(language in listOf("JSON","XML","HTML")) assertNotNull(parseJsonFormatterSnapshot(values+mapOf("language" to language)))
    }
}
