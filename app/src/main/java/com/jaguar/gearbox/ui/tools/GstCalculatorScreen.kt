package com.jaguar.gearbox.ui.tools

import androidx.compose.ui.res.stringResource
import com.jaguar.gearbox.R
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
        output = result?.let { context.getString(R.string.calc_base_amount_1_s_ntax_amount_2_s_ntotal_3_s, formatTrimmed(it.baseAmount, 2), formatTrimmed(it.taxAmount, 2), formatTrimmed(it.totalAmount, 2)) },
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
        title = context.getString(R.string.calc_gst_tax_calculator),
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
                    label = { Text(stringResource(R.string.calc_add_tax)) },
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = inclusive,
                    onClick = { inclusive = true },
                    label = { Text(stringResource(R.string.calc_remove_tax)) },
                    modifier = Modifier.weight(1f),
                )
            }

            Text(if (inclusive) context.getString(R.string.calc_extract_the_tax_already_included_in_the_amount) else context.getString(R.string.calc_calculate_tax_on_top_of_the_base_amount),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            DecimalField(
                value = amount,
                onValueChange = { amount = it },
                label = if (inclusive) context.getString(R.string.calc_amount_tax_included) else context.getString(R.string.calc_amount_before_tax),
                placeholder = context.getString(R.string.calc_e_g_1000),
                errorText = if (amount.isNotBlank() && amountValue == null) context.getString(R.string.calc_enter_a_valid_finite_non_negative_amount) else null,
            )
            Spacer(Modifier.height(12.dp))
            DecimalField(
                value = rate,
                onValueChange = { rate = it },
                label = context.getString(R.string.calc_tax_rate),
                errorText = if (rate.isNotBlank() && rateValue == null) context.getString(R.string.calc_enter_a_valid_finite_non_negative_rate) else null,
            )
        }, result = {
            if (result != null) {
                val summary = context.getString(R.string.calc_base_amount_1_s_n, formatTrimmed(result.baseAmount, 2)) +
                        context.getString(R.string.calc_tax_amount_1_s_n, formatTrimmed(result.taxAmount, 2)) +
                        context.getString(R.string.calc_total_1_s, formatTrimmed(result.totalAmount, 2))

                Spacer(Modifier.height(20.dp))
                Text(if (inclusive) context.getString(R.string.calc_before_tax) else context.getString(R.string.calc_including_tax), style = MaterialTheme.typography.labelLarge)
                Text(formatTrimmed(if (inclusive) result.baseAmount else result.totalAmount, 2), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                ResultCard(
                    text = summary,
                    onCopy = { context.copyToClipboard(context.getString(R.string.calc_gst), summary) },
                    onShare = { context.shareText(summary) },
                )
            } else if (amountValue != null && rateValue != null) {
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.calc_the_result_exceeds_the_supported_numeric_range_reduce_t), color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.calc_use_the_applicable_rate_the_default_is_not_a_tax_recomm), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            ToolActions(onClear = { amount = "" }, clearEnabled = amount.isNotEmpty())
        })
    }
}
