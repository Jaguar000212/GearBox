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
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgeCalculatorScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var laterDate by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    var earlierDate by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    // 0 = none, 1 = later, 2 = earlier
    var pickerTarget by rememberSaveable { mutableStateOf(0) }

    if (pickerTarget != 0) {
        val state = rememberDatePickerState()
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

        if (result != null) {
            Spacer(Modifier.height(16.dp))
            ResultCard(
                text = result,
                onCopy = { context.copyToClipboard("Age", result) },
                onShare = { context.shareText(result) },
            )
        } else if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(error, color = MaterialTheme.colorScheme.error)
        }
    }
}

private fun formatDate(date: LocalDate): String =
    "${date.dayOfMonth}/${date.monthValue}/${date.year}"

/**
 * Period.between does calendar-correct year/month/day math (respecting actual month lengths),
 * unlike a fixed 30-day borrow which is off by 1-2 days for most date pairs.
 */
private fun formatAge(earlier: LocalDate, later: LocalDate): String {
    val period = Period.between(earlier, later)
    return "${period.years} years, ${period.months} months, ${period.days} days"
}
