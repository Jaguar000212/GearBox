package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
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
import com.jaguar.gearbox.ui.components.ToolScaffold

@Composable
fun GstCalculatorScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var amount by rememberSaveable { mutableStateOf("") }
    var rate by rememberSaveable { mutableStateOf("18") }
    var inclusive by rememberSaveable { mutableStateOf(false) }

    val amountValue = amount.trim().toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }
    val rateValue = rate.trim().toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }

    ToolScaffold(
        title = "GST / Tax Calculator",
        icon = Tools.byRoute(Tools.ROUTE_GST)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
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

        Spacer(Modifier.height(16.dp))
        DecimalField(
            value = amount,
            onValueChange = { amount = it },
            label = if (inclusive) "Amount (tax included)" else "Amount (before tax)",
            placeholder = "e.g. 1000",
        )
        Spacer(Modifier.height(12.dp))
        DecimalField(
            value = rate,
            onValueChange = { rate = it },
            label = "Tax rate (%)",
        )

        if (amountValue != null && rateValue != null) {
            val result = calculateGst(amountValue, rateValue, inclusive)
            val summary = "Base amount: ${formatTrimmed(result.baseAmount, 2)}\n" +
                "Tax amount: ${formatTrimmed(result.taxAmount, 2)}\n" +
                "Total: ${formatTrimmed(result.totalAmount, 2)}"

            Spacer(Modifier.height(20.dp))
            ResultCard(
                text = summary,
                onCopy = { context.copyToClipboard("GST", summary) },
                onShare = { context.shareText(summary) },
            )
        } else if (amount.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text("Enter a valid amount and tax rate.", color = MaterialTheme.colorScheme.error)
        }
    }
}
