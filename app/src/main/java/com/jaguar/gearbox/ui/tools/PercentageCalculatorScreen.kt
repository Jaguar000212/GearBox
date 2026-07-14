package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
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
import com.jaguar.gearbox.logic.formatTrimmed
import com.jaguar.gearbox.ui.components.DecimalField
import com.jaguar.gearbox.ui.components.ResultCard
import com.jaguar.gearbox.ui.components.ToolScaffold

private enum class PercentMode(val label: String, val hint1: String, val hint2: String) {
    OF_VALUE("X% of Y", "Percentage (X)", "Value (Y)"),
    WHAT_PERCENT("X is what % of Y", "Value (X)", "Total (Y)"),
    CHANGE("% change from X to Y", "Original value (X)", "New value (Y)"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PercentageCalculatorScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var mode by rememberSaveable { mutableStateOf(PercentMode.OF_VALUE.name) }
    var modeExpanded by rememberSaveable { mutableStateOf(false) }
    val selectedMode = PercentMode.valueOf(mode)

    var field1 by rememberSaveable(selectedMode) { mutableStateOf("") }
    var field2 by rememberSaveable(selectedMode) { mutableStateOf("") }

    ToolScaffold(
        title = "Percentage Calculator",
        icon = Tools.byRoute(Tools.ROUTE_PERCENTAGE)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        ExposedDropdownMenuBox(
            expanded = modeExpanded,
            onExpandedChange = { modeExpanded = it },
        ) {
            OutlinedTextField(
                value = selectedMode.label,
                onValueChange = {},
                readOnly = true,
                label = { Text("Calculation") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modeExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
            )
            DropdownMenu(expanded = modeExpanded, onDismissRequest = { modeExpanded = false }) {
                PercentMode.entries.forEach { entry ->
                    DropdownMenuItem(
                        text = { Text(entry.label) },
                        onClick = {
                            mode = entry.name
                            modeExpanded = false
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        DecimalField(
            value = field1,
            onValueChange = { field1 = it },
            label = selectedMode.hint1,
        )
        Spacer(Modifier.height(12.dp))
        DecimalField(
            value = field2,
            onValueChange = { field2 = it },
            label = selectedMode.hint2,
        )

        val result = computePercentResult(selectedMode, field1, field2)
        if (result != null) {
            Spacer(Modifier.height(20.dp))
            ResultCard(
                text = result,
                onCopy = { context.copyToClipboard("Percentage", result) },
                onShare = { context.shareText(result) },
            )
        } else if (field1.isNotBlank() && field2.isNotBlank()) {
            // Both fields (not just one) must be filled in before flagging an error - otherwise
            // this fired as soon as the user typed into the first field, before touching the
            // second.
            Spacer(Modifier.height(12.dp))
            Text("Enter valid numbers.", color = MaterialTheme.colorScheme.error)
        }
    }
}

private fun computePercentResult(mode: PercentMode, f1: String, f2: String): String? {
    fun fmt(value: Double) = formatTrimmed(value, decimals = 4)

    val x = f1.trim().toDoubleOrNull()?.takeIf { it.isFinite() } ?: return null
    val y = f2.trim().toDoubleOrNull()?.takeIf { it.isFinite() } ?: return null

    return when (mode) {
        PercentMode.OF_VALUE -> "Result: ${fmt(x / 100 * y)}"
        PercentMode.WHAT_PERCENT -> {
            if (y == 0.0) return null
            "Result: ${fmt(x / y * 100)}%"
        }

        PercentMode.CHANGE -> {
            if (x == 0.0) return null
            val change = (y - x) / x * 100
            val direction = if (change >= 0) "increase" else "decrease"
            "Change: ${fmt(kotlin.math.abs(change))}% $direction"
        }
    }
}
