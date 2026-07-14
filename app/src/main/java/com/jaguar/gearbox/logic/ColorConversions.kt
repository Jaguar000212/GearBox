package com.jaguar.gearbox.logic

import kotlin.math.roundToInt

/** Parses a "#RRGGBB" or "RRGGBB" hex string into (red, green, blue) 0-255 components. */
fun parseHex(input: String): Triple<Int, Int, Int>? {
    val cleaned = input.trim().removePrefix("#")
    if (cleaned.length != 6 || cleaned.any { it !in "0123456789abcdefABCDEF" }) return null
    return try {
        val r = cleaned.substring(0, 2).toInt(16)
        val g = cleaned.substring(2, 4).toInt(16)
        val b = cleaned.substring(4, 6).toInt(16)
        Triple(r, g, b)
    } catch (_: NumberFormatException) {
        null
    }
}

/** Converts 0-255 RGB components to (hue 0-360, saturation %, lightness %). */
fun rgbToHsl(r: Int, g: Int, b: Int): Triple<Int, Int, Int> {
    val rf = r / 255f
    val gf = g / 255f
    val bf = b / 255f
    val max = maxOf(rf, gf, bf)
    val min = minOf(rf, gf, bf)
    val lightness = (max + min) / 2f

    if (max == min) return Triple(0, 0, (lightness * 100).roundToInt())

    val delta = max - min
    val saturation = if (lightness > 0.5f) delta / (2f - max - min) else delta / (max + min)
    val hue = when (max) {
        rf -> ((gf - bf) / delta + (if (gf < bf) 6f else 0f))
        gf -> ((bf - rf) / delta + 2f)
        else -> ((rf - gf) / delta + 4f)
    } * 60f

    return Triple(hue.roundToInt(), (saturation * 100).roundToInt(), (lightness * 100).roundToInt())
}
