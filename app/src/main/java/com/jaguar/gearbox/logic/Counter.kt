package com.jaguar.gearbox.logic

/** Null means the action exceeds the persisted Int range; never wrap the tally. */
fun nextCounterValue(count: Int, delta: Int): Int? = try {
    Math.addExact(count, delta)
} catch (_: ArithmeticException) {
    null
}
