package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.logic.computeAverageStatistics
import com.jaguar.gearbox.ui.components.ToolActions
import com.jaguar.gearbox.ui.components.ToolScaffold

@Composable
fun AverageCalculatorScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var input by rememberSaveable { mutableStateOf("") }
    var touched by rememberSaveable { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    val result = remember(input) { computeAverageStatistics(input) }
    val invalid = input.isNotBlank() && result == null && touched && !focused

    ToolScaffold(title = "Average Calculator", icon = Tools.byRoute(Tools.ROUTE_AVERAGE)!!.icon,
        onNavigateBack = onNavigateBack) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Numbers (comma separated)") },
            placeholder = { Text("e.g. 1, 2, 3, 4, 5") },
            supportingText = { Text(if (invalid)
                "Use finite numbers separated by commas. Check for invalid entries or values outside the supported range."
                else "Separate values with commas, for example: 1, 2, 3, 4, 5.") },
            isError = invalid,
            modifier = Modifier.fillMaxWidth().onFocusChanged {
                if (it.isFocused) touched = true
                focused = it.isFocused
            },
        )
        Spacer(Modifier.height(16.dp))
        if (result != null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Arithmetic mean", style = MaterialTheme.typography.labelLarge)
                    Text(result.arithmetic, style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary)
                    Text("Sum divided by ${result.count} values", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Other averages", style = MaterialTheme.typography.titleMedium)
            AverageStatistic("Geometric mean", result.geometric)
            AverageStatistic("Harmonic mean", result.harmonic)
            Spacer(Modifier.height(16.dp))
            Text("Summary", style = MaterialTheme.typography.titleMedium)
            AverageStatistic("Count", result.count.toString())
            AverageStatistic("Sum", result.sum)
            AverageStatistic("Minimum", result.smallest)
            AverageStatistic("Maximum", result.largest)
        } else {
            Text("Enter your numbers to see averages and summary statistics.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(16.dp))
        val export = result?.let { "Input: $input\n${it.asText().replaceFirst("Result:", "Arithmetic mean:")}" }
        ToolActions(
            onCopy = { export?.let { context.copyToClipboard("Average", it) } },
            onShare = { export?.let { context.shareText(it) } },
            resultEnabled = result != null,
            onClear = { input = ""; touched = false },
            clearEnabled = input.isNotEmpty(),
        )
    }
}

@Composable
private fun AverageStatistic(label: String, value: String) {
    // Stacked content avoids truncated values/explanations at large font scales.
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
