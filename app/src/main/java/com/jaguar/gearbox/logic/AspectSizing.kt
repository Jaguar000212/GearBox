package com.jaguar.gearbox.logic

import java.math.BigDecimal
import java.math.RoundingMode

enum class AspectSizingMode { RATIO, WIDTH, HEIGHT, PERCENT, FIT, FILL }
enum class AspectSizingError { INVALID_INPUT, OUTPUT_RANGE }
data class AspectSizingResult(val width: Int = 0, val height: Int = 0, val ratioWidth: Int = 0, val ratioHeight: Int = 0,
    val scaleNumerator: Long = 0, val scaleDenominator: Long = 1, val horizontalAdjustment: Int = 0,
    val verticalAdjustment: Int = 0, val error: AspectSizingError? = null) {
    fun scalePercent(): String = BigDecimal.valueOf(scaleNumerator).multiply(BigDecimal("100"))
        .divide(BigDecimal.valueOf(scaleDenominator), 4, RoundingMode.HALF_UP).toPlainString()
}
fun parsePixelDimension(text: String): Int? = text.takeIf { Regex("[1-9][0-9]{0,6}").matches(it) }
    ?.toIntOrNull()?.takeIf { it in 1..1000000 }
fun parseSizingPercent(text: String): BigDecimal? = text.takeIf { Regex("(?:0|[1-9][0-9]{0,4})(?:\\.[0-9]{1,2})?").matches(it) }
    ?.toBigDecimalOrNull()?.takeIf { it >= BigDecimal("0.01") && it <= BigDecimal("10000") }
private fun aspectGcd(first: Long, second: Long): Long {
    var a = first; var b = second
    while (b != 0L) { val remainder = a % b; a = b; b = remainder }
    return a
}
/** Pure sizing arithmetic only. FIT floors each dimension, FILL ceils; other modes round half up. */
fun calculateAspectSizing(sourceWidth: Int, sourceHeight: Int, targetWidth: Int, targetHeight: Int,
    percent: BigDecimal, mode: AspectSizingMode): AspectSizingResult {
    if (sourceWidth !in 1..1000000 || sourceHeight !in 1..1000000 ||
        (mode in setOf(AspectSizingMode.WIDTH, AspectSizingMode.FIT, AspectSizingMode.FILL) && targetWidth !in 1..1000000) ||
        (mode in setOf(AspectSizingMode.HEIGHT, AspectSizingMode.FIT, AspectSizingMode.FILL) && targetHeight !in 1..1000000) ||
        (mode == AspectSizingMode.PERCENT && (percent < BigDecimal("0.01") || percent > BigDecimal("10000") || percent.stripTrailingZeros().scale() > 2)))
        return AspectSizingResult(error = AspectSizingError.INVALID_INPUT)
    val ratioGcd = aspectGcd(sourceWidth.toLong(), sourceHeight.toLong()).toInt()
    val useWidth = targetWidth.toLong() * sourceHeight <= targetHeight.toLong() * sourceWidth
    val fraction = when (mode) {
        AspectSizingMode.RATIO -> 1L to 1L
        AspectSizingMode.WIDTH -> targetWidth.toLong() to sourceWidth.toLong()
        AspectSizingMode.HEIGHT -> targetHeight.toLong() to sourceHeight.toLong()
        AspectSizingMode.PERCENT -> percent.movePointRight(2).longValueExact() to 10000L
        AspectSizingMode.FIT -> if (useWidth) targetWidth.toLong() to sourceWidth.toLong() else targetHeight.toLong() to sourceHeight.toLong()
        AspectSizingMode.FILL -> if (!useWidth) targetWidth.toLong() to sourceWidth.toLong() else targetHeight.toLong() to sourceHeight.toLong()
    }
    val divisor = aspectGcd(fraction.first, fraction.second)
    val numerator = fraction.first / divisor; val denominator = fraction.second / divisor
    fun scaled(value: Int): Long {
        val product = value.toLong() * numerator
        return when (mode) {
            AspectSizingMode.FIT -> product / denominator
            AspectSizingMode.FILL -> (product + denominator - 1) / denominator
            else -> (product * 2 + denominator) / (2 * denominator)
        }
    }
    val width = scaled(sourceWidth); val height = scaled(sourceHeight)
    if (width !in 1..1000000 || height !in 1..1000000) return AspectSizingResult(error = AspectSizingError.OUTPUT_RANGE)
    val horizontal = when (mode) { AspectSizingMode.FIT -> targetWidth - width.toInt(); AspectSizingMode.FILL -> width.toInt() - targetWidth; else -> 0 }
    val vertical = when (mode) { AspectSizingMode.FIT -> targetHeight - height.toInt(); AspectSizingMode.FILL -> height.toInt() - targetHeight; else -> 0 }
    return AspectSizingResult(width.toInt(), height.toInt(), sourceWidth / ratioGcd, sourceHeight / ratioGcd,
        numerator, denominator, horizontal, vertical)
}
