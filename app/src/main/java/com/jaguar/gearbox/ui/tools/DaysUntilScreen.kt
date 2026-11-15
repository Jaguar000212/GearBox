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
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
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
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.SimplePrefsStore
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.ResultCard
import com.jaguar.gearbox.ui.components.ToolScaffold
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import com.jaguar.gearbox.logic.daysUntilSummary
import com.jaguar.gearbox.ui.components.rememberCurrentLocalDate

private const val KEY_LABEL = "days_until.label"
private const val KEY_TARGET_DATE = "days_until.target_date"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaysUntilScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val today by rememberCurrentLocalDate()
    val store = remember { SimplePrefsStore(context) }
    var label by rememberSaveable { mutableStateOf(store.getString(KEY_LABEL, "")) }
    var targetDate by rememberSaveable {
        mutableStateOf(
            store.getString(KEY_TARGET_DATE, "")
                .let { runCatching { LocalDate.parse(it) }.getOrNull() })
    }
    var showPicker by rememberSaveable { mutableStateOf(false) }

    fun persist(newLabel: String, newDate: LocalDate?) {
        store.edit {
            putString(KEY_LABEL, newLabel)
            putString(KEY_TARGET_DATE, newDate?.toString() ?: "")
        }
    }

    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = targetDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        targetDate =
                            Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        persist(label, targetDate)
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

    ToolScaffold(
        title = "Days Until",
        icon = Tools.byRoute(Tools.ROUTE_DAYS_UNTIL)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        Text("Count calendar days to a date, or days since it passed.",
            style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = label,
            onValueChange = {
                label = it
                persist(it, targetDate)
            },
            label = { Text("Event name (optional)") },
            placeholder = { Text("e.g. Diwali") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.DateRange, contentDescription = null)
            Text("  Target date: ${targetDate?.let { formatDate(it) } ?: "Choose date"}")
        }

        val target = targetDate
        if (target != null) {
            val summary = daysUntilSummary(today, target, label)

            Spacer(Modifier.height(20.dp))
            ResultCard(
                text = summary,
                textStyle = MaterialTheme.typography.headlineMedium,
                onCopy = { context.copyToClipboard("Days Until", daysUntilSummary(LocalDate.now(), target, label)) },
                onShare = { context.shareText(daysUntilSummary(LocalDate.now(), target, label)) },
            )
            Spacer(Modifier.height(8.dp))
            Text("From ${formatDate(today)} to ${formatDate(target)}",
                style = MaterialTheme.typography.bodyMedium)
        } else {
            Spacer(Modifier.height(20.dp))
            Text("Choose a target date to see your countdown. An event name is optional.",
                style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun formatDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
