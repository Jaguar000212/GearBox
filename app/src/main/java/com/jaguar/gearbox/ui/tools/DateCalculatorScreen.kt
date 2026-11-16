package com.jaguar.gearbox.ui.tools

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

private enum class DateUnit(val label: String) {
    DAYS("Days"),
    WEEKS("Weeks"),
    MONTHS("Months"),
    YEARS("Years"),
}

private enum class DateDirection(val label: String) {
    ADD("Add"),
    SUBTRACT("Subtract"),
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
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
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
        output = result?.let { "Result date: ${formatDate(it)}" },
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
        title = "Date Calculator",
        icon = Tools.byRoute(Tools.ROUTE_DATE_CALCULATOR)!!.icon,
        onNavigateBack = onNavigateBack,
        history = history,
    ) {
        AdaptiveToolLayout(input = {
            Text("Move forward or backward from a starting date.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.DateRange, contentDescription = null)
                Text("  Starting date: ${formatDate(baseDate)}")
            }

            Spacer(Modifier.height(16.dp))
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateDirection.entries.forEach { entry ->
                        FilterChip(selected = selectedDirection == entry,
                            onClick = { direction = entry.name }, label = { Text(entry.label) })
                    }
                }
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Number of ${selectedUnit.label.lowercase()}") },
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
                label = "Unit", selected = selectedUnit, choices = DateUnit.entries,
                choiceLabel = { it.label }, onSelect = { unit = it.name },
            )
        }, result = {
            if (result != null) {
                Spacer(Modifier.height(20.dp))
                Text("Resulting date", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                val summary = "${formatDate(baseDate)} • ${selectedDirection.label} $amount ${selectedUnit.label.lowercase()}\n${formatDate(result)}"
                ResultCard(
                    text = formatDate(result),
                    textStyle = MaterialTheme.typography.headlineMedium,
                    onCopy = { context.copyToClipboard("Date", summary) },
                    onShare = { context.shareText(summary) },
                )
                Spacer(Modifier.height(8.dp))
                Text("${selectedDirection.label} $amount ${selectedUnit.label.lowercase()} from ${formatDate(baseDate)}",
                    style = MaterialTheme.typography.bodyMedium)
            } else if (amountTouched && !amountFocused && amountInput.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Enter a valid amount that keeps the date in range.",
                    color = MaterialTheme.colorScheme.error
                )
            }
            Text("Clear blanks the amount and resets the starting date to today. Unit and direction stay unchanged.",
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
