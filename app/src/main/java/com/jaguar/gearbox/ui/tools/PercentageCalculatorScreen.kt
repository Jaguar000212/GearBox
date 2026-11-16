package com.jaguar.gearbox.ui.tools

import androidx.compose.ui.res.stringResource
import com.jaguar.gearbox.ui.components.ShowWorking
import com.jaguar.gearbox.R
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.key
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.logic.formatTrimmed
import com.jaguar.gearbox.ui.components.DecimalField
import com.jaguar.gearbox.logic.PercentageOperation
import com.jaguar.gearbox.logic.calculatePercentage
import com.jaguar.gearbox.ui.components.ToolActions
import com.jaguar.gearbox.ui.components.AdaptiveToolLayout
import com.jaguar.gearbox.ui.components.ToolScaffold
import com.jaguar.gearbox.ui.components.CompactChoiceSelector
import com.jaguar.gearbox.ui.components.rememberToolHistory

private enum class PercentMode(
    val label: Int, val hint1: Int, val hint2: Int,
    val example: Int, val operation: PercentageOperation,
) {
    OF_VALUE(R.string.calc_percentage_of_a_value, R.string.calc_percentage, R.string.calc_value,
        R.string.calc_example_15_of_240_36, PercentageOperation.OF_VALUE),
    WHAT_PERCENT(R.string.calc_what_percentage, R.string.calc_part, R.string.calc_total,
        R.string.calc_example_30_out_of_150_20, PercentageOperation.WHAT_PERCENT),
    CHANGE(R.string.calc_percentage_change, R.string.calc_original_value, R.string.calc_new_value,
        R.string.calc_example_80_to_100_25_increase, PercentageOperation.CHANGE),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PercentageCalculatorScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var mode by rememberSaveable { mutableStateOf(PercentMode.OF_VALUE.name) }
    val selectedMode = PercentMode.valueOf(mode)

    var field1 by rememberSaveable { mutableStateOf("") }
    var field2 by rememberSaveable { mutableStateOf("") }

    val historyX = field1.trim().toDoubleOrNull()?.takeIf { it.isFinite() }
    val historyY = field2.trim().toDoubleOrNull()?.takeIf { it.isFinite() }
    val historyResult = if (historyX != null && historyY != null) calculatePercentage(selectedMode.operation, historyX, historyY) else null

    val history = rememberToolHistory(
        route = Tools.ROUTE_PERCENTAGE,
        inputs = mapOf("mode" to mode, "field1" to field1, "field2" to field2),
        output = historyResult?.let { context.getString(R.string.calc_1_s_2_s_3_s, context.getString(selectedMode.label), formatTrimmed(it, decimals = 4), if (selectedMode == PercentMode.OF_VALUE) "" else "%") },
        modeInputs = mapOf("mode" to mode),
        onRestoreMode = { saved ->
            mode = PercentMode.entries.firstOrNull { it.name == saved["mode"] }?.name ?: PercentMode.OF_VALUE.name
        },
        onRestore = { saved ->
            mode = PercentMode.entries.firstOrNull { it.name == saved["mode"] }?.name ?: PercentMode.OF_VALUE.name
            field1 = saved["field1"] ?: ""
            field2 = saved["field2"] ?: ""
        },
    )

    ToolScaffold(
        title = context.getString(R.string.calc_percentage_calculator),
        icon = Tools.byRoute(Tools.ROUTE_PERCENTAGE)!!.icon,
        onNavigateBack = onNavigateBack,
        history = history,
    ) {
        val x = field1.trim().toDoubleOrNull()?.takeIf { it.isFinite() }
        val y = field2.trim().toDoubleOrNull()?.takeIf { it.isFinite() }
        val result = if (x != null && y != null) calculatePercentage(selectedMode.operation, x, y) else null
        AdaptiveToolLayout(input = {
            CompactChoiceSelector(
                label = context.getString(R.string.calc_calculation), selected = selectedMode, choices = PercentMode.entries,
                choiceLabel = { context.getString(it.label) }, onSelect = { entry ->
                    if (mode != entry.name) { field1 = ""; field2 = "" }
                    mode = entry.name
                },
            )

            Spacer(Modifier.height(12.dp))
            Text(context.getString(selectedMode.example), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))

            // Reset focus/validation state when the meaning of the inputs changes.
            key(selectedMode) {
                DecimalField(
                    value = field1,
                    onValueChange = { field1 = it },
                    label = context.getString(selectedMode.hint1),
                    errorText = if (selectedMode == PercentMode.CHANGE && x == 0.0)
                        context.getString(R.string.calc_original_value_must_not_be_zero) else null,
                )
                Spacer(Modifier.height(12.dp))
                DecimalField(
                    value = field2,
                    onValueChange = { field2 = it },
                    label = context.getString(selectedMode.hint2),
                    errorText = if (selectedMode == PercentMode.WHAT_PERCENT && y == 0.0)
                        context.getString(R.string.calc_total_must_not_be_zero) else null,
                )
            }

        }, result = {
            if (result != null && x != null && y != null) {
                fun fmt(value: Double) = formatTrimmed(value, decimals = 4)
                val answer = when (selectedMode) {
                    PercentMode.OF_VALUE -> fmt(result)
                    PercentMode.WHAT_PERCENT -> context.getString(R.string.calc_1_s, fmt(result))
                    PercentMode.CHANGE -> context.getString(R.string.calc_1_s, fmt(kotlin.math.abs(result)))
                }
                // Retain the existing signed-base percentage-change semantics.
                val detail = when (selectedMode) {
                    PercentMode.OF_VALUE -> context.getString(R.string.calc_1_s_of_2_s_3_s, fmt(x), fmt(y), answer)
                    PercentMode.WHAT_PERCENT -> context.getString(R.string.calc_1_s_out_of_2_s_3_s, fmt(x), fmt(y), answer)
                    PercentMode.CHANGE -> context.getString(R.string.calc_1_s_2_s_3_s_4_s, fmt(x), fmt(y), answer, if (result >= 0) context.getString(R.string.calc_increase) else context.getString(R.string.calc_decrease))
                }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.calc_result_2), style = MaterialTheme.typography.labelLarge)
                        Text(answer, style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary)
                        Text(detail, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                ShowWorking(percentageWorking(context, selectedMode.operation, x, y))
                Spacer(Modifier.height(12.dp))
                ToolActions(
                    onCopy = { context.copyToClipboard(context.getString(R.string.calc_percentage), detail) },
                    onShare = { context.shareText(detail) },
                )
            } else {
                Text(
                    if (x != null && y != null &&
                        !(selectedMode == PercentMode.CHANGE && x == 0.0) &&
                        !(selectedMode == PercentMode.WHAT_PERCENT && y == 0.0))
                        context.getString(R.string.calc_these_values_produce_a_result_outside_the_supported_ran)
                    else context.getString(R.string.calc_enter_both_values_to_see_your_result),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ToolActions(
                onClear = {
                    field1 = ""
                    field2 = ""
                    focusManager.clearFocus()
                },
                clearEnabled = field1.isNotEmpty() || field2.isNotEmpty(),
            )
        })
    }
}
