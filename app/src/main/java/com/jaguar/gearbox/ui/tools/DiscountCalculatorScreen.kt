package com.jaguar.gearbox.ui.tools

import androidx.compose.ui.res.stringResource
import com.jaguar.gearbox.ui.components.ShowWorking
import com.jaguar.gearbox.R
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
import com.jaguar.gearbox.ui.components.AdaptiveToolLayout
import com.jaguar.gearbox.ui.components.ToolActions
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
        output = historyResult?.let { context.getString(R.string.calc_final_price_1_s_nyou_save_2_s_3_s_effective, formatTrimmed(it.finalPrice, 2), formatTrimmed(it.totalSaved, 2), formatTrimmed(it.effectivePercent, 2)) },
        onRestore = { saved ->
            price = saved["price"] ?: ""
            discounts = saved["discounts"]?.split("\n")?.ifEmpty { listOf("") } ?: listOf("")
        },
    )

    ToolScaffold(
        title = context.getString(R.string.calc_discount_calculator),
        icon = Tools.byRoute(Tools.ROUTE_DISCOUNT)!!.icon,
        onNavigateBack = onNavigateBack,
        history = history,
    ) {
        AdaptiveToolLayout(input = {
            DecimalField(
                value = price,
                onValueChange = { price = it },
                label = context.getString(R.string.calc_original_price),
                placeholder = context.getString(R.string.calc_e_g_1500),
                errorText = if (price.isNotBlank() && originalPrice == null) context.getString(R.string.calc_enter_a_non_negative_price) else null,
            )

            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.calc_successive_discounts), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.calc_each_discount_applies_to_the_reduced_price_not_the_orig),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            discounts.forEachIndexed { index, value ->
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DecimalField(
                        value = value,
                        onValueChange = { newValue ->
                            discounts = discounts.toMutableList().also { it[index] = newValue }
                        },
                        label = context.getString(R.string.calc_discount_1_s, index + 1),
                        errorText = if (value.isNotBlank() && value.trim().toDoubleOrNull()?.let { it.isFinite() && it in 0.0..100.0 } != true)
                            context.getString(R.string.calc_enter_a_percentage_from_0_to_100) else null,
                        modifier = Modifier.weight(1f),
                    )
                    if (discounts.size > 1) {
                        IconButton(onClick = {
                            discounts = discounts.toMutableList().also { it.removeAt(index) }
                        }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = context.getString(R.string.calc_remove_discount_1_s, index + 1)
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
                Text(stringResource(R.string.calc_add_another_discount))
            }
        }, result = {
            if (originalPrice != null && allDiscountsValid && discountValues.isNotEmpty()) {
                val result = applyStackedDiscounts(originalPrice, discountValues)
                val summary = context.getString(R.string.calc_final_price_1_s_n, formatTrimmed(result.finalPrice, 2)) +
                        context.getString(R.string.calc_you_save_1_s_2_s_effective, formatTrimmed(
                                result.totalSaved,
                                2
                            ), formatTrimmed(result.effectivePercent, 2))

                Spacer(Modifier.height(20.dp))
                Text(stringResource(R.string.calc_final_price), style = MaterialTheme.typography.labelLarge)
                Text(formatTrimmed(result.finalPrice, 2), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                ResultCard(
                    text = summary,
                    onCopy = { context.copyToClipboard(context.getString(R.string.calc_discount), summary) },
                    onShare = { context.shareText(summary) },
                )
                ShowWorking(discountWorking(context, originalPrice, discountValues))
            }
            Spacer(Modifier.height(16.dp))
            ToolActions(onClear = { price = ""; discounts = listOf("") },
                clearEnabled = price.isNotEmpty() || discounts != listOf(""))
        })
    }
}
