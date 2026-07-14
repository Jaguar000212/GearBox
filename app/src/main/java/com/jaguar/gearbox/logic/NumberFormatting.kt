package com.jaguar.gearbox.logic

import java.util.Locale

/**
 * Formats [value] to [decimals] places, then trims trailing zeros (and a trailing dot) - e.g.
 * 7.0 -> "7", 3.140000 -> "3.14" - instead of `%f`'s fixed-width "7.000000". This was
 * reimplemented under a different name (`trimNumber`, `fmt`, `formatNumber`) in four separate
 * tool screens before being consolidated here.
 */
fun formatTrimmed(value: Double, decimals: Int = 6): String {
    val rounded = String.format(Locale.US, "%.${decimals}f", value).trimEnd('0').trimEnd('.')
    return rounded.ifEmpty { "0" }
}
