package com.jaguar.gearbox.logic

import java.util.Locale

/**
 * Formats [value] to [decimals] places, then trims trailing zeros (and a trailing dot) - e.g.
 * 7.0 -> "7", 3.140000 -> "3.14" - instead of `%f`'s fixed-width "7.000000". This was
 * reimplemented under a different name (`trimNumber`, `fmt`, `formatNumber`) in four separate
 * tool screens before being consolidated here.
 */
fun formatTrimmed(value: Double, decimals: Int = 6): String {
    require(decimals >= 0) { "Decimal places must be non-negative" }
    val formatted = String.format(Locale.US, "%.${decimals}f", value)
    val rounded = if (decimals == 0) formatted else formatted.trimEnd('0').trimEnd('.')
    return if (rounded == "-0") "0" else rounded.ifEmpty { "0" }
}

/**
 * Keeps normal six-place output, but never presents a finite nonzero value as zero.
 * Tiny results use scientific notation with six fractional mantissa places. This is a
 * display policy, not a change to arithmetic or the fixed-decimal contract of [formatTrimmed].
 */
fun formatNonzeroTrimmed(value: Double): String {
    val rounded = formatTrimmed(value)
    if (rounded != "0" || value == 0.0 || !value.isFinite()) return rounded
    val scientific = String.format(Locale.US, "%.6e", value).split('e')
    val mantissa = scientific[0].trimEnd('0').trimEnd('.')
    return "${mantissa}e${scientific[1].toInt()}"
}
