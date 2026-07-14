package com.jaguar.gearbox.logic

import kotlin.math.abs
import kotlin.math.floor

data class Fraction(val numerator: Long, val denominator: Long)

/**
 * Finds the fraction with the smallest denominator (up to 1,000,000) that approximates [value]
 * within a tight tolerance, via the standard continued-fraction expansion. This handles
 * arbitrarily long inputs and naturally recovers repeating decimals - e.g. 0.3333333333 resolves
 * to 1/3, 0.142857142857 to 1/7 - since those are exactly what a short continued fraction
 * converges to. Returns null only if [value] is too large to represent (its integer part alone
 * would overflow a Long numerator).
 */
fun decimalToFraction(value: Double): Fraction? {
    if (value == 0.0) return Fraction(0, 1)
    val absValue = abs(value)
    if (absValue > 1e15) return null

    val maxDenominator = 1_000_000L
    var h0 = 0L
    var h1 = 1L
    var k0 = 1L
    var k1 = 0L
    var b = absValue
    var bestNumerator = 0L
    var bestDenominator = 0L

    return try {
        for (i in 0 until 40) {
            val a = floor(b).toLong()
            val h2 = Math.addExact(Math.multiplyExact(a, h1), h0)
            val k2 = Math.addExact(Math.multiplyExact(a, k1), k0)
            if (k2 > maxDenominator) break
            h0 = h1; h1 = h2
            k0 = k1; k1 = k2
            bestNumerator = h1
            bestDenominator = k1
            val fractional = b - a
            if (fractional < 1e-12) break
            b = 1.0 / fractional
        }
        if (bestDenominator == 0L) null
        else Fraction(if (value < 0) -bestNumerator else bestNumerator, bestDenominator)
    } catch (_: ArithmeticException) {
        null
    }
}

/** Shows the improper fraction, plus a mixed-number form (e.g. "7/4  =  1 3/4") when it applies. */
fun formatFraction(fraction: Fraction): String {
    val isNegative = fraction.numerator < 0
    val sign = if (isNegative) "-" else ""
    val absNumerator = abs(fraction.numerator)
    val whole = absNumerator / fraction.denominator
    val remainder = absNumerator % fraction.denominator

    if (remainder == 0L) return "$sign$whole"

    val improper = "$sign$absNumerator/${fraction.denominator}"
    if (whole == 0L) return improper

    val mixed = "$sign$whole $remainder/${fraction.denominator}"
    return "$improper\n=  $mixed"
}
