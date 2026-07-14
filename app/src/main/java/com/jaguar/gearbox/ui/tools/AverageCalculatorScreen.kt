package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import com.jaguar.gearbox.ui.components.ToolScaffold
import java.util.Locale
import kotlin.math.pow

@Composable
fun AverageCalculatorScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var input by rememberSaveable { mutableStateOf("") }
    var output by rememberSaveable { mutableStateOf("") }

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
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { output = computeAverageResult(input) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Calculate") }

        if (output.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = output,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { input = ""; output = "" },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Clear, contentDescription = null)
                Text(" Clear")
            }
            OutlinedButton(
                onClick = { context.copyToClipboard("Copied Text", "Input: $input\n$output") },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null)
                Text(" Copy")
            }
            OutlinedButton(
                onClick = { context.shareText("Input: $input\n$output") },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Share, contentDescription = null)
                Text(" Share")
            }
        }
    }
}

/**
 * Replicates the statistics produced by the Java `AverageCalculator`: arithmetic mean, geometric
 * mean, harmonic mean, sum, count, largest and smallest.
 */
private fun computeAverageResult(input: String): String {
    val numbers = try {
        input.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { it.toDouble() }
    } catch (_: NumberFormatException) {
        return "Error: Format is not correct. Separate numbers with commas."
    }

    if (numbers.isEmpty()) {
        return "Error: Format is not correct. Separate numbers with commas."
    }

    val sum = numbers.sum()
    val average = sum / numbers.size
    val harmonicMean = numbers.size / numbers.sumOf { 1 / it }
    val geometricMean = numbers.fold(1.0) { acc, n -> acc * n }.pow(1.0 / numbers.size)
    val largest = numbers.max()
    val smallest = numbers.min()

    return String.format(
        Locale.US,
        "Result: %f\n\nGeometric Mean: %f\nHarmonic Mean: %f\n\nSum: %f\nCount: %d\nLargest: %f\nSmallest: %f",
        average, geometricMean, harmonicMean, sum, numbers.size, largest, smallest,
    )
}
