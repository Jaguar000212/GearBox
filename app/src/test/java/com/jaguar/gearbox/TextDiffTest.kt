package com.jaguar.gearbox

import com.jaguar.gearbox.logic.*
import org.junit.Assert.*
import org.junit.Test

class TextDiffTest {
    @Test fun equalAddedRemovedAndReplacementAreExplicit() {
        val result = compareTextLines("same\nold", "same\nnew")
        assertEquals(listOf(TextDiffLine(TextDiffKind.EQUAL, "same"), TextDiffLine(TextDiffKind.REMOVED, "old"),
            TextDiffLine(TextDiffKind.ADDED, "new")), result.lines)
        assertEquals(1, result.added); assertEquals(1, result.removed); assertEquals(1, result.equal)
    }
    @Test fun newlineConventionsNormalizeButFinalNewlineIsNotLost() {
        assertEquals(0, compareTextLines("a\r\nb\r", "a\nb\n").added)
        val result = compareTextLines("a", "a\n")
        assertEquals(TextDiffLine(TextDiffKind.ADDED, ""), result.lines.last())
        assertEquals(listOf(""), textDiffLines(""))
        assertEquals(listOf("", ""), textDiffLines("\n"))
    }
    @Test fun whitespaceCaseAndUnicodeAreNotIgnored() {
        val result = compareTextLines("a\n b\n\tc\n😀", "A\nb\nc\n😀")
        assertEquals(3, result.added); assertEquals(3, result.removed); assertEquals(1, result.equal)
    }
    @Test fun duplicatesProduceDeterministicConservedSequences() {
        val a = "x\ny\nx"; val b = "x\nx\ny"
        val result = compareTextLines(a, b)
        assertEquals(textDiffLines(a), result.lines.filter { it.kind != TextDiffKind.ADDED }.map { it.text })
        assertEquals(textDiffLines(b), result.lines.filter { it.kind != TextDiffKind.REMOVED }.map { it.text })
        assertEquals(result, compareTextLines(a, b))
    }
    @Test fun asciiWhitespaceComparisonPreservesBothOriginalLines() {
        val first = "  a\t  b \t\n\t \nsame"
        val second = "a b\n\nsame"
        val result = compareTextLines(first, second, TextDiffMode.IGNORE_ASCII_WHITESPACE)
        assertEquals(3, result.equal)
        assertEquals(2, result.ignoredWhitespace)
        assertEquals(0, result.added)
        assertEquals(0, result.removed)
        assertEquals("  a\t  b \t", result.lines[0].text)
        assertEquals("a b", result.lines[0].secondText)
        assertEquals("", result.lines[1].secondText)
        assertNull(result.lines[2].secondText)
        assertEquals(textDiffLines(first), result.lines.map { it.text })
        assertEquals(textDiffLines(second), result.lines.map { it.secondText ?: it.text })
    }
    @Test fun ignoredAsciiWhitespaceNeverIgnoresCaseUnicodeOrNewlines() {
        listOf("a" to "A", "a\u00a0b" to "a b", "a\u2003b" to "a b", "a\u000bb" to "a b", "ab" to "a b").forEach { (a, b) ->
            val result = compareTextLines(a, b, TextDiffMode.IGNORE_ASCII_WHITESPACE)
            assertEquals(1, result.added)
            assertEquals(1, result.removed)
        }
        assertEquals(1, compareTextLines("a", "a\n", TextDiffMode.IGNORE_ASCII_WHITESPACE).added)
        assertEquals(0, compareTextLines("a\r\nb", "a\nb", TextDiffMode.IGNORE_ASCII_WHITESPACE).ignoredWhitespace)
        assertEquals(compareTextLines(" a", "a"), compareTextLines(" a", "a", TextDiffMode.EXACT))
    }
    @Test fun whitespaceDuplicateAlignmentIsDeterministicAndConservesEachOriginal() {
        val first = " x\ny\nx "
        val second = "x\nx\t\ny"
        val result = compareTextLines(first, second, TextDiffMode.IGNORE_ASCII_WHITESPACE)
        assertEquals(textDiffLines(first), result.lines.filter { it.kind != TextDiffKind.ADDED }.map { it.text })
        assertEquals(textDiffLines(second), result.lines.filter { it.kind != TextDiffKind.REMOVED }.map { it.secondText ?: it.text })
        assertEquals(result, compareTextLines(first, second, TextDiffMode.IGNORE_ASCII_WHITESPACE))
        assertEquals(TextDiffError.TOO_LONG, compareTextLines(" ".repeat(16001), "", TextDiffMode.IGNORE_ASCII_WHITESPACE).error)
    }
    @Test fun boundsRejectWithoutTruncation() {
        assertNull(validateTextDiffInput("x".repeat(16000)))
        assertEquals(TextDiffError.TOO_LONG, compareTextLines("x".repeat(16001), "").error)
        assertNull(validateTextDiffInput(List(200) { "x" }.joinToString("\n")))
        assertEquals(TextDiffError.TOO_MANY_LINES, compareTextLines("\n".repeat(200), "").error)
        assertTrue(compareTextLines("x".repeat(16001), "").lines.isEmpty())
    }
    @Test fun maximumLcsTableRemainsBounded() {
        val result = compareTextLines(List(200) { "a$it" }.joinToString("\n"), List(200) { "b$it" }.joinToString("\n"))
        assertEquals(400, result.lines.size)
        assertEquals(200, result.added); assertEquals(200, result.removed)
    }
}
