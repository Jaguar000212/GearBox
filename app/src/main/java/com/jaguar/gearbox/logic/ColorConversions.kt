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

/** Validated HSL to 8-bit sRGB. 360 degrees is equivalent to 0; invalid values are rejected. */
fun hslToRgb(hue: Double, saturation: Double, lightness: Double): Triple<Int, Int, Int>? {
    if (!hue.isFinite() || !saturation.isFinite() || !lightness.isFinite() ||
        hue !in 0.0..360.0 || saturation !in 0.0..100.0 || lightness !in 0.0..100.0) return null
    val h = (hue % 360.0) / 60.0
    val s = saturation / 100.0
    val l = lightness / 100.0
    val chroma = (1.0 - kotlin.math.abs(2.0 * l - 1.0)) * s
    val x = chroma * (1.0 - kotlin.math.abs(h % 2.0 - 1.0))
    val m = l - chroma / 2.0
    val rgb = when {
        h < 1 -> Triple(chroma, x, 0.0)
        h < 2 -> Triple(x, chroma, 0.0)
        h < 3 -> Triple(0.0, chroma, x)
        h < 4 -> Triple(0.0, x, chroma)
        h < 5 -> Triple(x, 0.0, chroma)
        else -> Triple(chroma, 0.0, x)
    }
    fun component(value: Double) = ((value + m) * 255.0).roundToInt().coerceIn(0, 255)
    return Triple(component(rgb.first), component(rgb.second), component(rgb.third))
}
