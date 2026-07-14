package com.jaguar.gearbox.logic

import kotlin.random.Random

/**
 * Returns a random Long in [low, high] inclusive. Random.nextLong(from, until) takes an exclusive
 * upper bound, so the naive `high + 1` overflows to Long.MIN_VALUE when high == Long.MAX_VALUE,
 * which throws (until <= from). Shifting the range down by one avoids that overflow.
 */
fun randomLongInclusive(low: Long, high: Long): Long {
    if (low == high) return low
    return when {
        low == Long.MIN_VALUE && high == Long.MAX_VALUE -> Random.nextLong()
        high == Long.MAX_VALUE -> Random.nextLong(low - 1, high) + 1
        else -> Random.nextLong(low, high + 1)
    }
}
