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
