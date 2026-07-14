package com.jaguar.gearbox.logic

import java.util.Locale
import kotlin.math.abs

/** Simplifies a:b to lowest whole-number terms, scaling up first if either value has decimals. */
fun simplifyRatio(a: Double, b: Double): String {
    val decimalPlaces = maxOf(decimalPlacesOf(a), decimalPlacesOf(b))

    // Cap the scale so a*scale/b*scale can't overflow Long (Math.round() silently clamps to
    // Long.MAX_VALUE/MIN_VALUE on overflow instead of throwing, which would corrupt the ratio).
    val maxMagnitude = maxOf(abs(a), abs(b), 1.0)
    val maxSafeExponent = kotlin.math.floor(kotlin.math.log10(Long.MAX_VALUE / maxMagnitude / 10.0))
        .toInt()
        .coerceIn(0, decimalPlaces)
    val scale = Math.pow(10.0, maxSafeExponent.toDouble())

    var scaledA = Math.round(a * scale)
    var scaledB = Math.round(b * scale)

    // Safe now: scaledA/scaledB are bounded well within Long range by the exponent cap above, so
    // this can't hit the kotlin.math.abs(Long.MIN_VALUE) overflow that stays negative.
    val divisor = gcd(abs(scaledA), abs(scaledB))
    if (divisor != 0L) {
        scaledA /= divisor
        scaledB /= divisor
    }

    return "$scaledA : $scaledB"
}

fun decimalPlacesOf(value: Double): Int {
    val text = String.format(Locale.ROOT, "%.6f", value).trimEnd('0')
    val dotIndex = text.indexOf('.')
    return if (dotIndex == -1) 0 else text.length - dotIndex - 1
}

tailrec fun gcd(x: Long, y: Long): Long = if (y == 0L) x else gcd(y, x % y)
