package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalIconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
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

    ToolScaffold(
        title = "Tip Calculator",
        icon = Tools.byRoute(Tools.ROUTE_TIP)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        OutlinedTextField(
            value = bill,
            onValueChange = { bill = it },
            label = { Text("Bill amount") },
            placeholder = { Text("e.g. 45.00") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = tipPercent,
            onValueChange = { tipPercent = it },
            label = { Text("Tip %") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Split between", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(onClick = { if (splitCount > 1) splitCount-- }) {
                    Icon(Icons.Filled.Remove, contentDescription = "Fewer people")
                }
                Text(
                    text = splitCount.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                FilledTonalIconButton(onClick = { if (splitCount < 100) splitCount++ }) {
                    Icon(Icons.Filled.Add, contentDescription = "More people")
                }
            }
        }

        if (billAmount != null && tip != null) {
            val tipAmount = billAmount * tip / 100.0
            val total = billAmount + tipAmount
            val perPerson = total / splitCount
            val shareText = "Bill: ${formatCurrency(billAmount)}\n" +
                    "Tip amount: ${formatCurrency(tipAmount)}\n" +
                    "Total: ${formatCurrency(total)}\n" +
                    "Per person (split $splitCount ways): ${formatCurrency(perPerson)}"

            Spacer(Modifier.height(20.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)) {
                    ValueRow("Tip amount", formatCurrency(tipAmount))
                    Spacer(Modifier.height(8.dp))
                    ValueRow("Total", formatCurrency(total))
                    Spacer(Modifier.height(8.dp))
                    ValueRow("Per person", formatCurrency(perPerson))
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { context.copyToClipboard("Tip", shareText) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null)
                    Text(" Copy")
                }
                OutlinedButton(
                    onClick = { context.shareText(shareText) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                    Text(" Share")
                }
            }
        } else if (bill.isNotBlank()) {
            // tipPercent defaults to a non-blank "15", so only the bill field being touched
            // should trigger this — otherwise the error shows on an untouched, pristine screen.
            Spacer(Modifier.height(12.dp))
            Text("Enter a valid bill amount and tip %.", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun ValueRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun formatCurrency(value: Double): String =
    String.format(Locale.US, "%.2f", value)
