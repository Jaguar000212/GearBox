package com.jaguar.gearbox.ui.tools

import androidx.compose.ui.res.stringResource
import com.jaguar.gearbox.R
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.logic.computeAverageStatistics
import com.jaguar.gearbox.ui.components.ToolActions
import com.jaguar.gearbox.ui.components.AdaptiveToolLayout
import com.jaguar.gearbox.ui.components.ToolScaffold
import com.jaguar.gearbox.ui.components.rememberToolHistory

@Composable
fun AverageCalculatorScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var input by rememberSaveable { mutableStateOf("") }
    var touched by rememberSaveable { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    val result = remember(input) { computeAverageStatistics(input) }
    val invalid = input.isNotBlank() && result == null && touched && !focused

    val history = rememberToolHistory(
        route = Tools.ROUTE_AVERAGE,
        inputs = mapOf("input" to input),
        output = result?.let { localizedAverageResult(context, it) },
        onRestore = { saved ->
            input = saved["input"] ?: ""
        },
    )

    ToolScaffold(title = context.getString(R.string.calc_average_calculator), icon = Tools.byRoute(Tools.ROUTE_AVERAGE)!!.icon,
        onNavigateBack = onNavigateBack, history = history) {
        AdaptiveToolLayout(input = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text(stringResource(R.string.calc_numbers_comma_separated)) },
                placeholder = { Text(stringResource(R.string.calc_e_g_1_2_3_4_5)) },
                supportingText = { Text(if (invalid)
                    context.getString(R.string.calc_use_finite_numbers_separated_by_commas_check_for_invali)
                    else context.getString(R.string.calc_separate_values_with_commas_for_example_1_2_3_4_5)) },
                isError = invalid,
                modifier = Modifier.fillMaxWidth().onFocusChanged {
                    if (it.isFocused) touched = true
                    focused = it.isFocused
                },
            )
            Spacer(Modifier.height(16.dp))
        }, result = {
            if (result != null) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.calc_arithmetic_mean_2), style = MaterialTheme.typography.labelLarge)
                        Text(result.arithmetic, style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary)
                        Text(context.getString(R.string.calc_sum_divided_by_1_s_values, result.count), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.calc_other_averages), style = MaterialTheme.typography.titleMedium)
                AverageStatistic(context.getString(R.string.calc_geometric_mean), localizedAverageValue(context, result.geometric))
                AverageStatistic(context.getString(R.string.calc_harmonic_mean), localizedAverageValue(context, result.harmonic))
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.calc_summary), style = MaterialTheme.typography.titleMedium)
                AverageStatistic(context.getString(R.string.calc_count), result.count.toString())
                AverageStatistic(context.getString(R.string.calc_sum), result.sum)
                AverageStatistic(context.getString(R.string.calc_minimum), result.smallest)
                AverageStatistic(context.getString(R.string.calc_maximum), result.largest)
            } else {
                Text(stringResource(R.string.calc_enter_your_numbers_to_see_averages_and_summary_statisti),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(16.dp))
            val export = result?.let { context.getString(R.string.calc_input_1_s_n_2_s, input, localizedAverageResult(context, it)) }
            ToolActions(
                onCopy = { export?.let { context.copyToClipboard(context.getString(R.string.calc_average), it) } },
                onShare = { export?.let { context.shareText(it) } },
                resultEnabled = result != null,
                onClear = { input = ""; touched = false },
                clearEnabled = input.isNotEmpty(),
            )
        })
    }
}

@Composable
private fun AverageStatistic(label: String, value: String) {
    // Stacked content avoids truncated values/explanations at large font scales.
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

internal fun localizedAverageResult(context: android.content.Context, value: com.jaguar.gearbox.logic.AverageStatistics): String =
    context.getString(R.string.calc_average_summary, value.arithmetic,
        localizedAverageValue(context, value.geometric), localizedAverageValue(context, value.harmonic),
        value.sum, value.count, value.largest, value.smallest)

private fun localizedAverageValue(context: android.content.Context, value: String): String = when (value) {
    "N/A (list contains zero)" -> context.getString(R.string.calc_na_zero)
    "N/A (undefined or outside numeric range)" -> context.getString(R.string.calc_na_undefined)
    "N/A (list contains a negative number)" -> context.getString(R.string.calc_na_negative)
    "N/A (outside numeric range)" -> context.getString(R.string.calc_na_range)
    else -> value
}
