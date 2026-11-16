package com.jaguar.gearbox.ui.tools

import androidx.compose.ui.res.stringResource
import com.jaguar.gearbox.ui.components.ShowWorking
import com.jaguar.gearbox.R
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
import com.jaguar.gearbox.ui.components.AdaptiveToolLayout
import com.jaguar.gearbox.ui.components.ToolScaffold
import com.jaguar.gearbox.ui.components.rememberToolHistory
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

    val historyOutput = if (billAmount != null && calculatedTip != null && calculatedTotal != null && calculatedTip.isFinite() && calculatedTotal.isFinite()) {
        context.getString(R.string.calc_tip_amount_1_s_ntotal_2_s_nper_person_3_s, formatCurrency(calculatedTip), formatCurrency(calculatedTotal), formatCurrency(calculatedTotal / splitCount))
    } else null

    val history = rememberToolHistory(
        route = Tools.ROUTE_TIP,
        inputs = mapOf("bill" to bill, "tipPercent" to tipPercent, "splitCount" to splitCount.toString()),
        output = historyOutput,
        modeInputs = mapOf("tipPercent" to tipPercent, "splitCount" to splitCount.toString()),
        onRestoreMode = { saved ->
            tipPercent = saved["tipPercent"]?.takeIf { it.toDoubleOrNull()?.let { n -> n.isFinite() && n >= 0 } == true } ?: "15"
            splitCount = saved["splitCount"]?.toIntOrNull()?.coerceIn(1, 100) ?: 1
        },
        onRestore = { saved ->
            bill = saved["bill"] ?: ""
            tipPercent = saved["tipPercent"] ?: ""
            splitCount = saved["splitCount"]?.toIntOrNull()?.coerceIn(1, 100) ?: 1
        },
    )

    ToolScaffold(
        title = context.getString(R.string.calc_tip_calculator),
        icon = Tools.byRoute(Tools.ROUTE_TIP)!!.icon,
        onNavigateBack = onNavigateBack,
        history = history,
    ) {
        AdaptiveToolLayout(input = {
            Text(stringResource(R.string.calc_split_the_bill), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.calc_amounts_use_the_same_currency_as_your_bill), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            DecimalField(value = bill, onValueChange = { bill = it }, label = context.getString(R.string.calc_bill_amount), placeholder = context.getString(R.string.calc_e_g_45_00),
                errorText = if (bill.isNotBlank() && billAmount == null) context.getString(R.string.calc_enter_a_non_negative_bill_amount) else null)
            Spacer(Modifier.height(12.dp))
            DecimalField(value = tipPercent, onValueChange = { tipPercent = it }, label = context.getString(R.string.calc_tip_2),
                errorText = if (tipPercent.isNotBlank() && tip == null) context.getString(R.string.calc_enter_a_non_negative_tip_percentage) else null)

            Spacer(Modifier.height(16.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.calc_split_between), style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(onClick = { splitCount-- }, enabled = splitCount > 1) {
                        Icon(Icons.Filled.Remove, contentDescription = context.getString(R.string.calc_fewer_people))
                    }
                    Text(
                        text = splitCount.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    FilledTonalIconButton(onClick = { splitCount++ }, enabled = splitCount < 100) {
                        Icon(Icons.Filled.Add, contentDescription = context.getString(R.string.calc_more_people))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            androidx.compose.material3.TextButton(modifier = Modifier.fillMaxWidth(), onClick = { bill = ""; tipPercent = "15"; splitCount = 1 },
                enabled = bill.isNotEmpty() || tipPercent != "15" || splitCount != 1) {
                Icon(Icons.Filled.Clear, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.calc_clear_2))
            }
        }, result = {
            if (billAmount != null && calculatedTip != null && calculatedTotal != null &&
                calculatedTip.isFinite() && calculatedTotal.isFinite()
            ) {
                val tipAmount = calculatedTip
                val total = calculatedTotal
                val perPerson = total / splitCount
                val shareText = context.getString(R.string.calc_bill_1_s_n, formatCurrency(billAmount)) +
                        context.getString(R.string.calc_tip_amount_1_s_n, formatCurrency(tipAmount)) +
                        context.getString(R.string.calc_total_1_s_n, formatCurrency(total)) +
                        context.getString(R.string.calc_per_person_split_1_s_ways_2_s, splitCount, formatCurrency(perPerson))

                Text(stringResource(R.string.calc_per_person), style = MaterialTheme.typography.labelLarge)
                Text(formatCurrency(perPerson), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                ResultCard(
                    text = context.getString(R.string.calc_tip_1_s_ntotal_bill_with_tip_2_s_nsplit_between_3_s_peo, formatCurrency(tipAmount), formatCurrency(total), splitCount),
                    onCopy = { context.copyToClipboard(context.getString(R.string.calc_tip), shareText) },
                    onShare = { context.shareText(shareText) },
                )
                ShowWorking(tipWorking(context, billAmount, requireNotNull(tip), splitCount))
            } else if (billAmount != null && tip != null) {
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.calc_values_exceed_the_supported_numeric_range), color = MaterialTheme.colorScheme.error)
            }

        })
    }
}

private fun formatCurrency(value: Double): String =
    String.format(Locale.US, "%.2f", value)
