package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.ResultCard
import com.jaguar.gearbox.ui.components.ToolScaffold
import com.jaguar.gearbox.ui.components.rememberToolHistory
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgeCalculatorScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var laterDate by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    var earlierDate by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    // 0 = none, 1 = later, 2 = earlier
    var pickerTarget by rememberSaveable { mutableStateOf(0) }

    if (pickerTarget != 0) {
        val selectedDate = if (pickerTarget == 1) laterDate else earlierDate
        val state = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { pickerTarget = 0 },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val picked =
                            Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
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

    // Live compute, like the rest of the app's calculators - recomputes as soon as both dates
    // are picked instead of requiring a separate Calculate press (which could go stale after
    // reopening a date picker and changing a date).
    val later = laterDate
    val earlier = earlierDate
    val result = if (later != null && earlier != null && !later.isBefore(earlier)) {
        "Age: ${formatAge(earlier, later)}"
    } else {
        null
    }
    val error = if (later != null && earlier != null && later.isBefore(earlier)) {
        "Later date must not be before earlier date."
    } else {
        null
    }


    val history = rememberToolHistory(
        route = Tools.ROUTE_AGE,
        inputs = mapOf("earlierDate" to (earlierDate?.toString() ?: ""), "laterDate" to (laterDate?.toString() ?: "")),
        output = result,
        onRestore = { saved ->
            earlierDate = saved["earlierDate"]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            laterDate = saved["laterDate"]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        },
    )

    ToolScaffold(
        title = "Age Calculator",
        icon = Tools.byRoute(Tools.ROUTE_AGE)!!.icon,
        onNavigateBack = onNavigateBack,
        history = history,
    ) {
        Text(
            "Choose an earlier date and a later date to compare.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { pickerTarget = 2 }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.DateRange, contentDescription = null)
            Text("  Earlier date: ${earlierDate?.let { formatDate(it) } ?: "Choose date"}")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { pickerTarget = 1 }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.DateRange, contentDescription = null)
            Text("  Later date: ${laterDate?.let { formatDate(it) } ?: "Choose date"}")
        }

        if (result != null) {
            Spacer(Modifier.height(16.dp))
            ResultCard(
                text = result,
                textStyle = MaterialTheme.typography.headlineSmall,
                onCopy = { context.copyToClipboard("Age", result) },
                onShare = { context.shareText(result) },
            )
            Spacer(Modifier.height(8.dp))
            Text("Calendar years, months and days between ${formatDate(earlier!!)} and ${formatDate(later!!)}.",
                style = MaterialTheme.typography.bodyMedium)
        } else if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(error, color = MaterialTheme.colorScheme.error)
        } else {
            Spacer(Modifier.height(20.dp))
            Text("Your age difference will appear here once both dates are selected.",
                style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun formatDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

/**
 * Period.between does calendar-correct year/month/day math (respecting actual month lengths),
 * unlike a fixed 30-day borrow which is off by 1-2 days for most date pairs.
 */
private fun formatAge(earlier: LocalDate, later: LocalDate): String {
    val period = Period.between(earlier, later)
    return "${period.years} years, ${period.months} months, ${period.days} days"
}
