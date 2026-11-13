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
