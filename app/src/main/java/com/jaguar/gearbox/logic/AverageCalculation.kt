package com.jaguar.gearbox.logic

/**
 * Replicates the statistics produced by the Java `AverageCalculator`: arithmetic mean, geometric
 * mean, harmonic mean, sum, count, largest and smallest. Returns null (rather than an error
 * string) on invalid input, so the caller can show the error as plain text instead of inside a
 * card styled as a successful result.
 */
fun computeAverageResult(input: String): String? {
    val numbers = try {
        input.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { it.toDouble() }
    } catch (_: NumberFormatException) {
        return null
    }

    if (numbers.isEmpty() || numbers.any { !it.isFinite() }) return null

    val sum = numbers.sum()
    if (!sum.isFinite()) return null
    val average = sum / numbers.size

    // Harmonic mean divides by each value, so any zero in the list makes it undefined (the naive
    // formula silently gives 0, which looks like a valid answer rather than an error).
    val harmonicMeanText = if (numbers.any { it == 0.0 }) {
        "N/A (list contains zero)"
    } else {
        val reciprocalSum = numbers.sumOf { 1 / it }
        val harmonicMean = numbers.size / reciprocalSum
        if (!reciprocalSum.isFinite() || reciprocalSum == 0.0 || !harmonicMean.isFinite()) {
            "N/A (undefined or outside numeric range)"
        } else formatTrimmed(harmonicMean)
    }

    // A fractional power of a negative base is NaN in Java/Kotlin regardless of whether the
    // implied root would be odd, so a negative number also makes the geometric mean undefined.
    val geometricMeanText = if (numbers.any { it < 0.0 }) {
        "N/A (list contains a negative number)"
    } else if (numbers.any { it == 0.0 }) {
        "0"
    } else {
        // Log-space avoids overflowing the product for ordinary large, positive inputs.
        val geometricMean = kotlin.math.exp(numbers.sumOf { kotlin.math.ln(it) } / numbers.size)
        if (geometricMean.isFinite()) formatTrimmed(geometricMean)
        else "N/A (outside numeric range)"
    }

    val largest = numbers.max()
    val smallest = numbers.min()

    return "Result: ${formatTrimmed(average)}\n\n" +
            "Geometric Mean: $geometricMeanText\nHarmonic Mean: $harmonicMeanText\n\n" +
            "Sum: ${formatTrimmed(sum)}\nCount: ${numbers.size}\n" +
            "Largest: ${formatTrimmed(largest)}\nSmallest: ${formatTrimmed(smallest)}"
}

