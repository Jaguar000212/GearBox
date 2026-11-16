package com.jaguar.gearbox.ui.tools

import androidx.compose.ui.res.stringResource
import com.jaguar.gearbox.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.focus.onFocusChanged
import com.jaguar.gearbox.ui.components.ResultCard
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.AdaptiveToolLayout
import com.jaguar.gearbox.ui.components.ToolActions
import com.jaguar.gearbox.ui.components.ToolScaffold
import com.jaguar.gearbox.ui.components.CompactChoiceSelector
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.jaguar.gearbox.ui.components.rememberToolHistory
import com.jaguar.gearbox.ui.components.LocalDateSaver
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private enum class DateUnit(val label: Int) {
    DAYS(R.string.calc_days),
    WEEKS(R.string.calc_weeks),
    MONTHS(R.string.calc_months),
    YEARS(R.string.calc_years),
}

private enum class DateDirection(val label: Int) {
    ADD(R.string.calc_add),
    SUBTRACT(R.string.calc_subtract),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DateCalculatorScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var baseDate by rememberSaveable(stateSaver = LocalDateSaver) { mutableStateOf(LocalDate.now()) }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var amountTouched by rememberSaveable { mutableStateOf(false) }
    var amountFocused by rememberSaveable { mutableStateOf(false) }
    var amountInput by rememberSaveable { mutableStateOf("1") }
    var unit by rememberSaveable { mutableStateOf(DateUnit.DAYS.name) }
    var direction by rememberSaveable { mutableStateOf(DateDirection.ADD.name) }

    val selectedUnit = DateUnit.valueOf(unit)
    val selectedDirection = DateDirection.valueOf(direction)

    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = baseDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        baseDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showPicker = false
                }) { Text(stringResource(R.string.calc_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.calc_cancel)) }
            },
        ) {
            DatePicker(state = state)
        }
    }

    val amount = amountInput.trim().toLongOrNull()
    val result = if (amount != null) {
        try {
            computeResultDate(baseDate, amount, selectedUnit, selectedDirection)
        } catch (_: java.time.DateTimeException) {
            null // out of LocalDate's representable year range
        } catch (_: ArithmeticException) {
            null // amount too large for the underlying day-count math to add without overflowing
        }
    } else {
        null
    }


    val history = rememberToolHistory(
        route = Tools.ROUTE_DATE_CALCULATOR,
        inputs = mapOf("baseDate" to baseDate.toString(), "amountInput" to amountInput, "unit" to unit, "direction" to direction),
        output = result?.let { context.getString(R.string.calc_result_date_1_s, formatDate(it)) },
        modeInputs = mapOf("unit" to unit, "direction" to direction),
        onRestoreMode = { saved ->
            unit = DateUnit.entries.firstOrNull { it.name == saved["unit"] }?.name ?: DateUnit.DAYS.name
            direction = DateDirection.entries.firstOrNull { it.name == saved["direction"] }?.name ?: DateDirection.ADD.name
        },
        onRestore = { saved ->
            saved["baseDate"]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }?.let { baseDate = it }
            amountInput = saved["amountInput"] ?: "1"
            unit = DateUnit.entries.firstOrNull { it.name == saved["unit"] }?.name ?: DateUnit.DAYS.name
            direction = DateDirection.entries.firstOrNull { it.name == saved["direction"] }?.name ?: DateDirection.ADD.name
        },
    )

    ToolScaffold(
        title = context.getString(R.string.calc_date_calculator),
        icon = Tools.byRoute(Tools.ROUTE_DATE_CALCULATOR)!!.icon,
        onNavigateBack = onNavigateBack,
        history = history,
    ) {
        AdaptiveToolLayout(input = {
            Text(stringResource(R.string.calc_move_forward_or_backward_from_a_starting_date), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.DateRange, contentDescription = null)
                Text(context.getString(R.string.calc_starting_date_1_s, formatDate(baseDate)))
            }

            Spacer(Modifier.height(16.dp))
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateDirection.entries.forEach { entry ->
                        FilterChip(selected = selectedDirection == entry,
                            onClick = { direction = entry.name }, label = { Text(context.getString(entry.label)) })
                    }
                }
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text(context.getString(R.string.calc_number_of_1_s, context.getString(selectedUnit.label).lowercase())) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().onFocusChanged {
                        if (amountFocused && !it.isFocused) amountTouched = true
                        amountFocused = it.isFocused
                    },
                )
            }

            Spacer(Modifier.height(12.dp))
            CompactChoiceSelector(
                label = context.getString(R.string.calc_unit), selected = selectedUnit, choices = DateUnit.entries,
                choiceLabel = { context.getString(it.label) }, onSelect = { unit = it.name },
            )
        }, result = {
            if (result != null) {
                Spacer(Modifier.height(20.dp))
                Text(stringResource(R.string.calc_resulting_date), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                val summary = context.getString(R.string.calc_1_s_2_s_3_s_4_s_n_5_s, formatDate(baseDate), context.getString(selectedDirection.label), amount, context.getString(selectedUnit.label).lowercase(), formatDate(result))
                ResultCard(
                    text = formatDate(result),
                    textStyle = MaterialTheme.typography.headlineMedium,
                    onCopy = { context.copyToClipboard(context.getString(R.string.calc_date), summary) },
                    onShare = { context.shareText(summary) },
                )
                Spacer(Modifier.height(8.dp))
                Text(context.getString(R.string.calc_1_s_2_s_3_s_from_4_s, context.getString(selectedDirection.label), amount, context.getString(selectedUnit.label).lowercase(), formatDate(baseDate)),
                    style = MaterialTheme.typography.bodyMedium)
            } else if (amountTouched && !amountFocused && amountInput.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    context.getString(R.string.calc_enter_a_valid_amount_that_keeps_the_date_in_range),
                    color = MaterialTheme.colorScheme.error
                )
            }
            Text(stringResource(R.string.calc_clear_blanks_the_amount_and_resets_the_starting_date_to),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ToolActions(onClear = {
                baseDate = LocalDate.now(); amountInput = ""; amountTouched = false; showPicker = false
            }, clearEnabled = amountInput.isNotEmpty() || baseDate != LocalDate.now())
        })
    }
}

private fun formatDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

private fun computeResultDate(
    base: LocalDate,
    amount: Long,
    unit: DateUnit,
    direction: DateDirection
): LocalDate {
    val signedAmount = if (direction == DateDirection.SUBTRACT) Math.negateExact(amount) else amount
    return when (unit) {
        DateUnit.DAYS -> base.plusDays(signedAmount)
        DateUnit.WEEKS -> base.plusWeeks(signedAmount)
        DateUnit.MONTHS -> base.plusMonths(signedAmount)
        DateUnit.YEARS -> base.plusYears(signedAmount)
    }
}
