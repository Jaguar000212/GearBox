package com.jaguar.gearbox.logic

import java.math.BigInteger

private val ones = arrayOf(
    "",
    "One",
    "Two",
    "Three",
    "Four",
    "Five",
    "Six",
    "Seven",
    "Eight",
    "Nine",
    "Ten",
    "Eleven",
    "Twelve",
    "Thirteen",
    "Fourteen",
    "Fifteen",
    "Sixteen",
    "Seventeen",
    "Eighteen",
    "Nineteen",
)
private val tens = arrayOf(
    "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety",
)

/** Converts a number into English words using the short-scale (million, billion) grouping. */
fun numberToWords(number: Long): String {
    if (number == 0L) return "Zero"

    val isNegative = number < 0
    // BigInteger.abs() avoids the Long.MIN_VALUE overflow that kotlin.math.abs() has (its magnitude
    // has no positive Long representation), and the grouping below never overflows either.
    var magnitude = BigInteger.valueOf(number).abs()
    val thousand = BigInteger.valueOf(1000)

    val groups = mutableListOf<Int>()
    while (magnitude > BigInteger.ZERO) {
        groups.add(magnitude.mod(thousand).toInt())
        magnitude /= thousand
    }

    // Long.MAX_VALUE has 19 digits, i.e. up to 7 groups of three (index 0-6), so this must cover
    // through "Quintillion" or grouping into it throws ArrayIndexOutOfBoundsException.
    val scales =
        arrayOf("", "Thousand", "Million", "Billion", "Trillion", "Quadrillion", "Quintillion")
    val parts = mutableListOf<String>()
    for (i in groups.indices.reversed()) {
        val group = groups[i]
        if (group == 0) continue
        val groupWords = threeDigitsToWords(group)
        parts.add(if (scales[i].isNotEmpty()) "$groupWords ${scales[i]}" else groupWords)
    }

    return (if (isNegative) "Negative " else "") + parts.joinToString(" ")
}

/**
 * Converts using the Indian numbering system (Thousand, Lakh, Crore, ...) instead of the
 * short-scale grouping in [numberToWords] - the app's Tambola audience reads 1,23,45,000 as
 * "One Crore Twenty-Three Lakh...", not "Twelve Million...".
 */
fun numberToWordsIndian(number: Long): String {
    if (number == 0L) return "Zero"

    val isNegative = number < 0
    var magnitude = BigInteger.valueOf(number).abs()
    val thousand = BigInteger.valueOf(1000)
    val hundred = BigInteger.valueOf(100)

    // Only the last three digits ever get a "hundred" - every group above that is a plain
    // two-digit (00-99) group multiplied by its scale (thousand, lakh, crore, ...).
    val firstGroup = magnitude.mod(thousand).toInt()
    magnitude /= thousand

    val higherGroups = mutableListOf<Int>()
    while (magnitude > BigInteger.ZERO) {
        higherGroups.add(magnitude.mod(hundred).toInt())
        magnitude /= hundred
    }

    // Covers up to Shankh, which is more than enough for Long.MAX_VALUE (19 digits).
    val scales = arrayOf("", "Thousand", "Lakh", "Crore", "Arab", "Kharab", "Neel", "Padma", "Shankh")
    val parts = mutableListOf<String>()
    for (i in higherGroups.indices.reversed()) {
        val group = higherGroups[i]
        if (group == 0) continue
        parts.add("${twoDigitsToWords(group)} ${scales[i + 1]}")
    }
    if (firstGroup != 0) parts.add(threeDigitsToWords(firstGroup))

    return (if (isNegative) "Negative " else "") + parts.joinToString(" ")
}

private fun twoDigitsToWords(number: Int): String = when {
    number in 1..19 -> ones[number]
    number >= 20 -> {
        val tensPart = tens[number / 10]
        val onesPart = ones[number % 10]
        if (onesPart.isNotEmpty()) "$tensPart-$onesPart" else tensPart
    }
    else -> ""
}

private fun threeDigitsToWords(number: Int): String {
    val hundreds = number / 100
    val remainder = number % 100

    val parts = mutableListOf<String>()
    if (hundreds > 0) parts.add("${ones[hundreds]} Hundred")
    if (remainder in 1..19) {
        parts.add(ones[remainder])
    } else if (remainder >= 20) {
        val tensPart = tens[remainder / 10]
        val onesPart = ones[remainder % 10]
        parts.add(if (onesPart.isNotEmpty()) "$tensPart-$onesPart" else tensPart)
    }
    return parts.joinToString(" ")
}
