package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.ResultCard
import com.jaguar.gearbox.ui.components.ToolScaffold
import java.util.Locale
import kotlin.math.pow

@Composable
fun AverageCalculatorScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var input by rememberSaveable { mutableStateOf("") }

    // Live compute, like the rest of the app's calculators - a button here previously left a
    // stale result on screen after editing the input, still showing the answer to a list you'd
    // already changed.
    val output = computeAverageResult(input)

    ToolScaffold(
        title = "Average Calculator",
        icon = Tools.byRoute(Tools.ROUTE_AVERAGE)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Numbers (comma separated)") },
            placeholder = { Text("e.g. 4, 8, 15, 16, 23, 42") },
            modifier = Modifier.fillMaxWidth(),
        )

        if (output != null) {
            Spacer(Modifier.height(16.dp))
            ResultCard(
                text = output,
                onCopy = { context.copyToClipboard("Average", "Input: $input\n$output") },
                onShare = { context.shareText("Input: $input\n$output") },
            )
        } else if (input.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Enter numbers separated by commas, e.g. 4, 8, 15.",
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { input = "" },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Clear, contentDescription = null)
                Text(" Clear")
            }
        }
    }
}

/**
 * Replicates the statistics produced by the Java `AverageCalculator`: arithmetic mean, geometric
 * mean, harmonic mean, sum, count, largest and smallest. Returns null (rather than an error
 * string) on invalid input, so the caller can show the error as plain text instead of inside a
 * card styled as a successful result.
 */
private fun computeAverageResult(input: String): String? {
    val numbers = try {
        input.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { it.toDouble() }
    } catch (_: NumberFormatException) {
        return null
    }

    if (numbers.isEmpty()) return null

    val sum = numbers.sum()
    val average = sum / numbers.size

    // Harmonic mean divides by each value, so any zero in the list makes it undefined (the naive
    // formula silently gives 0, which looks like a valid answer rather than an error).
    val harmonicMeanText = if (numbers.any { it == 0.0 }) {
        "N/A (list contains zero)"
    } else {
        formatNumber(numbers.size / numbers.sumOf { 1 / it })
    }

    // A fractional power of a negative base is NaN in Java/Kotlin regardless of whether the
    // implied root would be odd, so a negative number also makes the geometric mean undefined.
    val geometricMeanText = if (numbers.any { it < 0.0 }) {
        "N/A (list contains a negative number)"
    } else {
        formatNumber(numbers.fold(1.0) { acc, n -> acc * n }.pow(1.0 / numbers.size))
    }

    val largest = numbers.max()
    val smallest = numbers.min()

    return "Result: ${formatNumber(average)}\n\n" +
            "Geometric Mean: $geometricMeanText\nHarmonic Mean: $harmonicMeanText\n\n" +
            "Sum: ${formatNumber(sum)}\nCount: ${numbers.size}\n" +
            "Largest: ${formatNumber(largest)}\nSmallest: ${formatNumber(smallest)}"
}

/** Formats without %f's fixed trailing zeros (e.g. "7.000000"), matching the app's other tools. */
private fun formatNumber(value: Double): String {
    val rounded = String.format(Locale.US, "%.6f", value).trimEnd('0').trimEnd('.')
    return rounded.ifEmpty { "0" }
}
