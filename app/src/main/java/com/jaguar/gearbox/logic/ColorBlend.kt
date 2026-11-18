package com.jaguar.gearbox.logic

import java.util.Locale

/** Exactly six opaque sRGB hex digits with a leading #; no alpha or shorthand. */
fun parseOpaqueHex(text: String): RgbColor? {
    if (!Regex("#[0-9a-fA-F]{6}").matches(text)) return null
    val packed = text.substring(1).toInt(16)
    return RgbColor(packed shr 16 and 255, packed shr 8 and 255, packed and 255)
}

/** Encoded-channel interpolation, NOT linear-light mixing, paint mixing or alpha compositing.
 * Integer arithmetic gives exact whole-percent weights; ties round upward to the nearest 8-bit channel.
 */
fun blendEncodedSrgb(first: RgbColor, second: RgbColor, secondPercent: Int): RgbColor {
    require(secondPercent in 0..100)
    fun channel(a: Int, b: Int) = (a * (100 - secondPercent) + b * secondPercent + 50) / 100
    return RgbColor(channel(first.red, second.red), channel(first.green, second.green), channel(first.blue, second.blue))
}

fun colorBlendHex(color: RgbColor): String = String.format(Locale.ROOT, "#%02X%02X%02X", color.red, color.green, color.blue)
