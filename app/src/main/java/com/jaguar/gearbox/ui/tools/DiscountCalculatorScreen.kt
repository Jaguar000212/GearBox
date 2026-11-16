package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.jaguar.gearbox.logic.applyStackedDiscounts
import com.jaguar.gearbox.logic.formatTrimmed
import com.jaguar.gearbox.ui.components.DecimalField
import com.jaguar.gearbox.ui.components.ResultCard
import com.jaguar.gearbox.ui.components.StringListSaver
import com.jaguar.gearbox.ui.components.ToolScaffold
import com.jaguar.gearbox.ui.components.rememberToolHistory

@Composable
fun DiscountCalculatorScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var price by rememberSaveable { mutableStateOf("") }
    var discounts by rememberSaveable(stateSaver = StringListSaver) { mutableStateOf(listOf("")) }

    val originalPrice = price.trim().toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }
    val filledDiscounts = discounts.filter { it.isNotBlank() }
    val discountValues = filledDiscounts.mapNotNull {
        it.trim().toDoubleOrNull()?.takeIf { v -> v.isFinite() && v in 0.0..100.0 }
    }
    val allDiscountsValid = discountValues.size == filledDiscounts.size

    val historyResult = if (originalPrice != null && allDiscountsValid && discountValues.isNotEmpty())
        applyStackedDiscounts(originalPrice, discountValues) else null

    val history = rememberToolHistory(
        route = Tools.ROUTE_DISCOUNT,
        inputs = mapOf("price" to price, "discounts" to discounts.joinToString("\n")),
        output = historyResult?.let { "Final price: ${formatTrimmed(it.finalPrice, 2)}\nYou save: ${formatTrimmed(it.totalSaved, 2)} (${formatTrimmed(it.effectivePercent, 2)}% effective)" },
        onRestore = { saved ->
            price = saved["price"] ?: ""
            discounts = saved["discounts"]?.split("\n")?.take(100)?.ifEmpty { listOf("") } ?: listOf("")
        },
    )

    ToolScaffold(
        title = "Discount Calculator",
        icon = Tools.byRoute(Tools.ROUTE_DISCOUNT)!!.icon,
        onNavigateBack = onNavigateBack,
        history = history,
    ) {
        DecimalField(
            value = price,
            onValueChange = { price = it },
            label = "Original price",
            placeholder = "e.g. 1500",
            errorText = if (price.isNotBlank() && originalPrice == null) "Enter a non-negative price." else null,
        )

        Spacer(Modifier.height(16.dp))
        Text("Successive discounts", style = MaterialTheme.typography.titleMedium)
        Text("Each discount applies to the reduced price, not the original price. For example, 20% then 10% saves 28%, not 30%.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        discounts.forEachIndexed { index, value ->
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                DecimalField(
                    value = value,
                    onValueChange = { newValue ->
                        discounts = discounts.toMutableList().also { it[index] = newValue }
                    },
                    label = "Discount ${index + 1} (%)",
                    errorText = if (value.isNotBlank() && value.trim().toDoubleOrNull()?.let { it.isFinite() && it in 0.0..100.0 } != true)
                        "Enter a percentage from 0 to 100." else null,
                    modifier = Modifier.weight(1f),
                )
                if (discounts.size > 1) {
                    IconButton(onClick = {
                        discounts = discounts.toMutableList().also { it.removeAt(index) }
                    }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Remove discount ${index + 1}"
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { discounts = discounts + "" },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(" Add another discount")
        }

        if (originalPrice != null && allDiscountsValid && discountValues.isNotEmpty()) {
            val result = applyStackedDiscounts(originalPrice, discountValues)
            val summary = "Final price: ${formatTrimmed(result.finalPrice, 2)}\n" +
                    "You save: ${
                        formatTrimmed(
                            result.totalSaved,
                            2
                        )
                    } (${formatTrimmed(result.effectivePercent, 2)}% effective)"

            Spacer(Modifier.height(20.dp))
            Text("Final price", style = MaterialTheme.typography.labelLarge)
            Text(formatTrimmed(result.finalPrice, 2), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            ResultCard(
                text = summary,
                onCopy = { context.copyToClipboard("Discount", summary) },
                onShare = { context.shareText(summary) },
            )
        }
        Spacer(Modifier.height(16.dp))
        androidx.compose.material3.TextButton(modifier = Modifier.fillMaxWidth(), onClick = { price = ""; discounts = listOf("") },
            enabled = price.isNotEmpty() || discounts != listOf("")) {
            Icon(Icons.Filled.Clear, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Clear")
        }
    }
}
