package com.jaguar.gearbox.logic

/** The three existing percentage operations, independent of their presentation. */
enum class PercentageOperation { OF_VALUE, WHAT_PERCENT, CHANGE }

fun calculatePercentage(operation: PercentageOperation, first: Double, second: Double): Double? {
    if (!first.isFinite() || !second.isFinite()) return null
    return when (operation) {
        PercentageOperation.OF_VALUE -> first / 100 * second
        PercentageOperation.WHAT_PERCENT -> if (second == 0.0) return null else first / second * 100
        PercentageOperation.CHANGE -> if (first == 0.0) return null else (second - first) / first * 100
    }.takeIf { it.isFinite() }
}
