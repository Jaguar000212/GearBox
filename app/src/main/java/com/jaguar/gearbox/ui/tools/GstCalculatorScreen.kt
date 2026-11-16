package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.jaguar.gearbox.logic.calculateGst
import com.jaguar.gearbox.logic.formatTrimmed
import com.jaguar.gearbox.ui.components.DecimalField
import com.jaguar.gearbox.ui.components.ResultCard
import com.jaguar.gearbox.ui.components.AdaptiveToolLayout
import com.jaguar.gearbox.ui.components.ToolActions
import com.jaguar.gearbox.ui.components.ToolScaffold
import com.jaguar.gearbox.ui.components.rememberToolHistory

@Composable
fun GstCalculatorScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var amount by rememberSaveable { mutableStateOf("") }
    var rate by rememberSaveable { mutableStateOf("18") }
    var inclusive by rememberSaveable { mutableStateOf(false) }

    val amountValue = amount.trim().toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }
    val rateValue = rate.trim().toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }
    val result = if (amountValue != null && rateValue != null) {
        calculateGst(amountValue, rateValue, inclusive)
    } else null


    val history = rememberToolHistory(
        route = Tools.ROUTE_GST,
        inputs = mapOf("amount" to amount, "rate" to rate, "inclusive" to inclusive.toString()),
        output = result?.let { "Base amount: ${formatTrimmed(it.baseAmount, 2)}\nTax amount: ${formatTrimmed(it.taxAmount, 2)}\nTotal: ${formatTrimmed(it.totalAmount, 2)}" },
        modeInputs = mapOf("inclusive" to inclusive.toString()),
        onRestoreMode = { saved ->
            inclusive = saved["inclusive"]?.toBooleanStrictOrNull() ?: false
        },
        onRestore = { saved ->
            amount = saved["amount"] ?: ""
            rate = saved["rate"] ?: ""
            inclusive = saved["inclusive"]?.toBooleanStrictOrNull() ?: false
        },
    )

    ToolScaffold(
        title = "GST / Tax Calculator",
        icon = Tools.byRoute(Tools.ROUTE_GST)!!.icon,
        onNavigateBack = onNavigateBack,
        history = history,
    ) {
        AdaptiveToolLayout(input = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = !inclusive,
                    onClick = { inclusive = false },
                    label = { Text("Add tax") },
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = inclusive,
                    onClick = { inclusive = true },
                    label = { Text("Remove tax") },
                    modifier = Modifier.weight(1f),
                )
            }

            Text(if (inclusive) "Extract the tax already included in the amount." else "Calculate tax on top of the base amount.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            DecimalField(
                value = amount,
                onValueChange = { amount = it },
                label = if (inclusive) "Amount (tax included)" else "Amount (before tax)",
                placeholder = "e.g. 1000",
                errorText = if (amount.isNotBlank() && amountValue == null) "Enter a valid, finite, non-negative amount." else null,
            )
            Spacer(Modifier.height(12.dp))
            DecimalField(
                value = rate,
                onValueChange = { rate = it },
                label = "Tax rate (%)",
                errorText = if (rate.isNotBlank() && rateValue == null) "Enter a valid, finite, non-negative rate." else null,
            )
        }, result = {
            if (result != null) {
                val summary = "Base amount: ${formatTrimmed(result.baseAmount, 2)}\n" +
                        "Tax amount: ${formatTrimmed(result.taxAmount, 2)}\n" +
                        "Total: ${formatTrimmed(result.totalAmount, 2)}"

                Spacer(Modifier.height(20.dp))
                Text(if (inclusive) "Before tax" else "Including tax", style = MaterialTheme.typography.labelLarge)
                Text(formatTrimmed(if (inclusive) result.baseAmount else result.totalAmount, 2), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                ResultCard(
                    text = summary,
                    onCopy = { context.copyToClipboard("GST", summary) },
                    onShare = { context.shareText(summary) },
                )
            } else if (amountValue != null && rateValue != null) {
                Spacer(Modifier.height(12.dp))
                Text("The result exceeds the supported numeric range. Reduce the amount or tax rate.", color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(16.dp))
            Text("Use the applicable rate; the default is not a tax recommendation.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            ToolActions(onClear = { amount = "" }, clearEnabled = amount.isNotEmpty())
        })
    }
}
