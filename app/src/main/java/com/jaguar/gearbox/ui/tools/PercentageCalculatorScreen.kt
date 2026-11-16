package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.key
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.logic.formatTrimmed
import com.jaguar.gearbox.ui.components.DecimalField
import com.jaguar.gearbox.logic.PercentageOperation
import com.jaguar.gearbox.logic.calculatePercentage
import com.jaguar.gearbox.ui.components.ToolActions
import com.jaguar.gearbox.ui.components.AdaptiveToolLayout
import com.jaguar.gearbox.ui.components.ToolScaffold
import com.jaguar.gearbox.ui.components.CompactChoiceSelector
import com.jaguar.gearbox.ui.components.rememberToolHistory

private enum class PercentMode(
    val label: String, val hint1: String, val hint2: String,
    val example: String, val operation: PercentageOperation,
) {
    OF_VALUE("Percentage of a value", "Percentage", "Value",
        "Example: 15% of 240 = 36", PercentageOperation.OF_VALUE),
    WHAT_PERCENT("What percentage?", "Part", "Total",
        "Example: 30 out of 150 = 20%", PercentageOperation.WHAT_PERCENT),
    CHANGE("Percentage change", "Original value", "New value",
        "Example: 80 to 100 = 25% increase", PercentageOperation.CHANGE),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PercentageCalculatorScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var mode by rememberSaveable { mutableStateOf(PercentMode.OF_VALUE.name) }
    val selectedMode = PercentMode.valueOf(mode)

    var field1 by rememberSaveable { mutableStateOf("") }
    var field2 by rememberSaveable { mutableStateOf("") }

    val historyX = field1.trim().toDoubleOrNull()?.takeIf { it.isFinite() }
    val historyY = field2.trim().toDoubleOrNull()?.takeIf { it.isFinite() }
    val historyResult = if (historyX != null && historyY != null) calculatePercentage(selectedMode.operation, historyX, historyY) else null

    val history = rememberToolHistory(
        route = Tools.ROUTE_PERCENTAGE,
        inputs = mapOf("mode" to mode, "field1" to field1, "field2" to field2),
        output = historyResult?.let { "${selectedMode.label}: ${formatTrimmed(it, decimals = 4)}${if (selectedMode == PercentMode.OF_VALUE) "" else "%"}" },
        modeInputs = mapOf("mode" to mode),
        onRestoreMode = { saved ->
            mode = PercentMode.entries.firstOrNull { it.name == saved["mode"] }?.name ?: PercentMode.OF_VALUE.name
        },
        onRestore = { saved ->
            mode = PercentMode.entries.firstOrNull { it.name == saved["mode"] }?.name ?: PercentMode.OF_VALUE.name
            field1 = saved["field1"] ?: ""
            field2 = saved["field2"] ?: ""
        },
    )

    ToolScaffold(
        title = "Percentage Calculator",
        icon = Tools.byRoute(Tools.ROUTE_PERCENTAGE)!!.icon,
        onNavigateBack = onNavigateBack,
        history = history,
    ) {
        val x = field1.trim().toDoubleOrNull()?.takeIf { it.isFinite() }
        val y = field2.trim().toDoubleOrNull()?.takeIf { it.isFinite() }
        val result = if (x != null && y != null) calculatePercentage(selectedMode.operation, x, y) else null
        AdaptiveToolLayout(input = {
            CompactChoiceSelector(
                label = "Calculation", selected = selectedMode, choices = PercentMode.entries,
                choiceLabel = { it.label }, onSelect = { entry ->
                    if (mode != entry.name) { field1 = ""; field2 = "" }
                    mode = entry.name
                },
            )

            Spacer(Modifier.height(12.dp))
            Text(selectedMode.example, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))

            // Reset focus/validation state when the meaning of the inputs changes.
            key(selectedMode) {
                DecimalField(
                    value = field1,
                    onValueChange = { field1 = it },
                    label = selectedMode.hint1,
                    errorText = if (selectedMode == PercentMode.CHANGE && x == 0.0)
                        "Original value must not be zero." else null,
                )
                Spacer(Modifier.height(12.dp))
                DecimalField(
                    value = field2,
                    onValueChange = { field2 = it },
                    label = selectedMode.hint2,
                    errorText = if (selectedMode == PercentMode.WHAT_PERCENT && y == 0.0)
                        "Total must not be zero." else null,
                )
            }

        }, result = {
            if (result != null && x != null && y != null) {
                fun fmt(value: Double) = formatTrimmed(value, decimals = 4)
                val answer = when (selectedMode) {
                    PercentMode.OF_VALUE -> fmt(result)
                    PercentMode.WHAT_PERCENT -> "${fmt(result)}%"
                    PercentMode.CHANGE -> "${fmt(kotlin.math.abs(result))}%"
                }
                // Retain the existing signed-base percentage-change semantics.
                val detail = when (selectedMode) {
                    PercentMode.OF_VALUE -> "${fmt(x)}% of ${fmt(y)} = $answer"
                    PercentMode.WHAT_PERCENT -> "${fmt(x)} out of ${fmt(y)} = $answer"
                    PercentMode.CHANGE -> "${fmt(x)} → ${fmt(y)}: $answer ${if (result >= 0) "increase" else "decrease"}"
                }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Result", style = MaterialTheme.typography.labelLarge)
                        Text(answer, style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary)
                        Text(detail, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Spacer(Modifier.height(12.dp))
                ToolActions(
                    onCopy = { context.copyToClipboard("Percentage", detail) },
                    onShare = { context.shareText(detail) },
                )
            } else {
                Text(
                    if (x != null && y != null &&
                        !(selectedMode == PercentMode.CHANGE && x == 0.0) &&
                        !(selectedMode == PercentMode.WHAT_PERCENT && y == 0.0))
                        "These values produce a result outside the supported range. Try smaller values."
                    else "Enter both values to see your result.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        })
    }
}
