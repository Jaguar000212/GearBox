package com.jaguar.gearbox.logic

import kotlin.math.pow

/** Opaque 8-bit sRGB, not arbitrary display RGB or a composited translucent color. */
data class RgbColor(val red: Int, val green: Int, val blue: Int) {
    init { require(red in 0..255 && green in 0..255 && blue in 0..255) }
}

/**
 * WCAG sRGB relative luminance in [0, 1], using the 0.04045 transfer-function breakpoint.
 * Source: https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html
 * The earlier 0.03928 breakpoint was corrected in May 2021.
 */
fun relativeLuminance(red: Int, green: Int, blue: Int): Double = relativeLuminance(RgbColor(red, green, blue))

fun relativeLuminance(color: RgbColor): Double {
    fun linear(channel: Int): Double {
        val encoded = channel / 255.0
        return if (encoded <= 0.04045) encoded / 12.92 else ((encoded + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * linear(color.red) + 0.7152 * linear(color.green) + 0.0722 * linear(color.blue)
}

/** Unrounded opaque-sRGB contrast ratio in [1, 21], symmetric in its two colors. */
fun contrastRatio(foreground: RgbColor, background: RgbColor): Double {
    val first = relativeLuminance(foreground)
    val second = relativeLuminance(background)
    return (maxOf(first, second) + 0.05) / (minOf(first, second) + 0.05)
}

/** Use the unrounded ratio at thresholds; a displayed 4.50 may still be below 4.5. */
fun meetsAaNormalText(ratio: Double): Boolean = ratio.isFinite() && ratio >= 4.5
fun meetsAaLargeText(ratio: Double): Boolean = ratio.isFinite() && ratio >= 3.0
