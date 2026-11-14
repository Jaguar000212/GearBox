package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.DecimalField
import com.jaguar.gearbox.ui.components.ResultCard
import com.jaguar.gearbox.ui.components.ToolScaffold
import java.util.Locale

@Composable
fun TipCalculatorScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var bill by rememberSaveable { mutableStateOf("") }
    var tipPercent by rememberSaveable { mutableStateOf("15") }
    var splitCount by rememberSaveable { mutableStateOf(1) }

    val billAmount = bill.trim().toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }
    val tip = tipPercent.trim().toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }
    val calculatedTip = if (billAmount != null && tip != null) billAmount * (tip / 100.0) else null
    val calculatedTotal = if (billAmount != null && calculatedTip != null) billAmount + calculatedTip else null

    ToolScaffold(
        title = "Tip Calculator",
        icon = Tools.byRoute(Tools.ROUTE_TIP)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        Text("Split the bill", style = MaterialTheme.typography.titleMedium)
        Text("Amounts use the same currency as your bill.", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        DecimalField(value = bill, onValueChange = { bill = it }, label = "Bill amount", placeholder = "e.g. 45.00",
            errorText = if (bill.isNotBlank() && billAmount == null) "Enter a non-negative bill amount." else null)
        Spacer(Modifier.height(12.dp))
        DecimalField(value = tipPercent, onValueChange = { tipPercent = it }, label = "Tip (%)",
            errorText = if (tipPercent.isNotBlank() && tip == null) "Enter a non-negative tip percentage." else null)

        Spacer(Modifier.height(16.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Split between", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(onClick = { splitCount-- }, enabled = splitCount > 1) {
                    Icon(Icons.Filled.Remove, contentDescription = "Fewer people")
                }
                Text(
                    text = splitCount.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                FilledTonalIconButton(onClick = { splitCount++ }, enabled = splitCount < 100) {
                    Icon(Icons.Filled.Add, contentDescription = "More people")
                }
            }
        }

        if (billAmount != null && calculatedTip != null && calculatedTotal != null &&
            calculatedTip.isFinite() && calculatedTotal.isFinite()
        ) {
            val tipAmount = calculatedTip
            val total = calculatedTotal
            val perPerson = total / splitCount
            val shareText = "Bill: ${formatCurrency(billAmount)}\n" +
                    "Tip amount: ${formatCurrency(tipAmount)}\n" +
                    "Total: ${formatCurrency(total)}\n" +
                    "Per person (split $splitCount ways): ${formatCurrency(perPerson)}"

            Spacer(Modifier.height(20.dp))
            Text("Per person", style = MaterialTheme.typography.labelLarge)
            Text(formatCurrency(perPerson), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            ResultCard(
                text = "Tip: ${formatCurrency(tipAmount)}\nTotal bill with tip: ${formatCurrency(total)}\nSplit between $splitCount people",
                onCopy = { context.copyToClipboard("Tip", shareText) },
                onShare = { context.shareText(shareText) },
            )
        } else if (billAmount != null && tip != null) {
            Spacer(Modifier.height(12.dp))
            Text("Values exceed the supported numeric range.", color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
        androidx.compose.material3.TextButton(modifier = Modifier.fillMaxWidth(), onClick = { bill = ""; tipPercent = "15"; splitCount = 1 },
            enabled = bill.isNotEmpty() || tipPercent != "15" || splitCount != 1) {
            Icon(Icons.Filled.Clear, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Clear")
        }
    }
}

private fun formatCurrency(value: Double): String =
    String.format(Locale.US, "%.2f", value)
