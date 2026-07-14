package com.jaguar.gearbox.logic

import org.junit.Assert.assertEquals
import org.junit.Test

class NumberWordsTest {

    @Test
    fun `zero is Zero`() {
        assertEquals("Zero", numberToWords(0L))
    }

    @Test
    fun `simple numbers`() {
        assertEquals("Forty-Two", numberToWords(42L))
        assertEquals("One Hundred Five", numberToWords(105L))
        assertEquals("Negative Seven", numberToWords(-7L))
    }

    @Test
    fun `long max value does not throw and covers all scale words`() {
        // Long.MAX_VALUE has 19 digits, i.e. 7 groups of three - the original bug threw
        // ArrayIndexOutOfBoundsException here because `scales` only had enough entries for 5.
        val words = numberToWords(Long.MAX_VALUE)
        assertEquals(
            "Nine Quintillion Two Hundred Twenty-Three Quadrillion " +
                    "Three Hundred Seventy-Two Trillion Thirty-Six Billion Eight Hundred " +
                    "Fifty-Four Million Seven Hundred Seventy-Five Thousand Eight Hundred Seven",
            words
        )
    }

    @Test
    fun `long min value does not throw despite having no positive counterpart`() {
        // kotlin.math.abs(Long.MIN_VALUE) silently returns Long.MIN_VALUE again (still negative);
        // the fix uses BigInteger.abs() instead, which has no such overflow.
        val words = numberToWords(Long.MIN_VALUE)
        assert(words.startsWith("Negative Nine Quintillion"))
    }

    @Test
    fun `indian system groups by lakh and crore instead of million and billion`() {
        assertEquals(
            "One Crore Twenty-Three Lakh Forty-Five Thousand",
            numberToWordsIndian(12345000L)
        )
        assertEquals("Zero", numberToWordsIndian(0L))
        assertEquals("Negative Fifty", numberToWordsIndian(-50L))
    }

    @Test
    fun `indian system handles long max value without throwing`() {
        numberToWordsIndian(Long.MAX_VALUE)
        numberToWordsIndian(Long.MIN_VALUE)
    }
}
