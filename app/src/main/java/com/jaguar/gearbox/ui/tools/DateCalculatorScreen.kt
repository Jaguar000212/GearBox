package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.ToolScaffold
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateCalculatorScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var baseDate by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    var showPicker by remember { mutableStateOf(false) }
    var amountInput by rememberSaveable { mutableStateOf("1") }
    var unit by rememberSaveable { mutableStateOf(DateUnit.DAYS.name) }
    var unitExpanded by rememberSaveable { mutableStateOf(false) }
    var direction by rememberSaveable { mutableStateOf(DateDirection.ADD.name) }
    var directionExpanded by rememberSaveable { mutableStateOf(false) }

    val selectedUnit = DateUnit.valueOf(unit)
    val selectedDirection = DateDirection.valueOf(direction)

    if (showPicker) {
        val state = rememberDatePickerState()
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
    val result = if (amount != null && baseDate != null) {
        try {
            computeResultDate(baseDate!!, amount, selectedUnit, selectedDirection)
        } catch (_: java.time.DateTimeException) {
            null // out of LocalDate's representable year range
        } catch (_: ArithmeticException) {
            null // amount too large for the underlying day-count math to add without overflowing
        }
    } else {
        null
    }

    ToolScaffold(
        title = "Date Calculator",
        icon = Tools.byRoute(Tools.ROUTE_DATE_CALCULATOR)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.DateRange, contentDescription = null)
            Text("  Date: ${baseDate?.let { formatDate(it) } ?: "select"}")
        }

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ExposedDropdownMenuBox(
                expanded = directionExpanded,
                onExpandedChange = { directionExpanded = it },
                modifier = Modifier.weight(1f),
            ) {
                OutlinedTextField(
                    value = selectedDirection.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Direction") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = directionExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                )
                DropdownMenu(
                    expanded = directionExpanded,
                    onDismissRequest = { directionExpanded = false }) {
                    DateDirection.entries.forEach { entry ->
                        DropdownMenuItem(
                            text = { Text(entry.label) },
                            onClick = {
                                direction = entry.name
                                directionExpanded = false
                            },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = amountInput,
                onValueChange = { amountInput = it },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(12.dp))
        ExposedDropdownMenuBox(
            expanded = unitExpanded,
            onExpandedChange = { unitExpanded = it },
        ) {
            OutlinedTextField(
                value = selectedUnit.label,
                onValueChange = {},
                readOnly = true,
                label = { Text("Unit") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
            )
            DropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                DateUnit.entries.forEach { entry ->
                    DropdownMenuItem(
                        text = { Text(entry.label) },
                        onClick = {
                            unit = entry.name
                            unitExpanded = false
                        },
                    )
                }
            }
        }

        if (result != null) {
            Spacer(Modifier.height(20.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = formatDate(result),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(onClick = { context.copyToClipboard("Date", formatDate(result)) }) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null)
                    Text(" Copy")
                }
            }
        } else if (amountInput.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Enter a valid amount that keeps the date in range.",
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

private fun formatDate(date: LocalDate): String =
    "${date.dayOfMonth}/${date.monthValue}/${date.year}"

private fun computeResultDate(
    base: LocalDate,
    amount: Long,
    unit: DateUnit,
    direction: DateDirection
): LocalDate {
    val signedAmount = if (direction == DateDirection.SUBTRACT) -amount else amount
    return when (unit) {
        DateUnit.DAYS -> base.plusDays(signedAmount)
        DateUnit.WEEKS -> base.plusWeeks(signedAmount)
        DateUnit.MONTHS -> base.plusMonths(signedAmount)
        DateUnit.YEARS -> base.plusYears(signedAmount)
    }
}
