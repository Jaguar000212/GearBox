package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.ToolScaffold
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgeCalculatorScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var laterDate by remember { mutableStateOf<LocalDate?>(null) }
    var earlierDate by remember { mutableStateOf<LocalDate?>(null) }
    var result by remember { mutableStateOf("") }
    // 0 = none, 1 = later, 2 = earlier
    var pickerTarget by remember { mutableStateOf(0) }

    if (pickerTarget != 0) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { pickerTarget = 0 },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        if (pickerTarget == 1) laterDate = picked else earlierDate = picked
                    }
                    pickerTarget = 0
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { pickerTarget = 0 }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = state)
        }
    }

    ToolScaffold(
        title = "Age Calculator",
        icon = Tools.byRoute(Tools.ROUTE_AGE)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        Text(
            "Age = Later date − Earlier date",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { pickerTarget = 1 }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.DateRange, contentDescription = null)
            Text("  Later date: ${laterDate?.let { formatDate(it) } ?: "select"}")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { pickerTarget = 2 }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.DateRange, contentDescription = null)
            Text("  Earlier date: ${earlierDate?.let { formatDate(it) } ?: "select"}")
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { result = calculateAge(laterDate, earlierDate) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Calculate") }

        if (result.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = result,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(onClick = { context.copyToClipboard("Age", result) }) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null)
                    Text(" Copy")
                }
            }
        }
    }
}

private fun formatDate(date: LocalDate): String =
    "${date.dayOfMonth}/${date.monthValue}/${date.year}"

/**
 * Replicates the age math from the Java `AgeCalculator` (simple per-field delta with a 30-day
 * month borrow), operating on the later date minus the earlier date.
 */
private fun calculateAge(later: LocalDate?, earlier: LocalDate?): String {
    if (later == null || earlier == null) {
        return "Error: Format is not correct. Select dates properly."
    }

    var deltaYear = later.year - earlier.year
    var deltaMonth = later.monthValue - earlier.monthValue
    var deltaDay = later.dayOfMonth - earlier.dayOfMonth

    if (deltaDay < 0) {
        deltaMonth--
        deltaDay += 30
    }
    if (deltaMonth < 0) {
        deltaYear--
        deltaMonth += 12
    }

    return "Age: $deltaYear years, $deltaMonth months, $deltaDay days"
}
