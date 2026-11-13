package com.jaguar.gearbox.logic

import java.math.BigDecimal

/**
 * Simplifies finite a:b to whole-number terms without rounding tiny values to zero or clamping
 * large values to Long limits. BigDecimal.valueOf uses each Double's decimal representation;
 * this cannot recover precision already lost when input text was parsed as a Double.
 * A ratio of zero to zero, or a non-finite input, is undefined and returns null.
 */
fun simplifyRatio(a: Double, b: Double): String? {
    if (!a.isFinite() || !b.isFinite() || (a == 0.0 && b == 0.0)) return null
    val decimalA = BigDecimal.valueOf(a)
    val decimalB = BigDecimal.valueOf(b)
    val scale = maxOf(decimalA.scale(), decimalB.scale())
    val integerA = decimalA.movePointRight(scale).toBigIntegerExact()
    val integerB = decimalB.movePointRight(scale).toBigIntegerExact()
    val divisor = integerA.abs().gcd(integerB.abs())
    return "${integerA / divisor} : ${integerB / divisor}"
}

tailrec fun gcd(x: Long, y: Long): Long = if (y == 0L) x else gcd(y, x % y)
