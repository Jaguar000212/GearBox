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
import java.time.temporal.ChronoUnit

private const val KEY_LABEL = "days_until.label"
private const val KEY_TARGET_DATE = "days_until.target_date"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaysUntilScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { SimplePrefsStore(context) }
    var label by rememberSaveable { mutableStateOf(store.getString(KEY_LABEL, "")) }
    var targetDate by rememberSaveable {
        mutableStateOf(store.getString(KEY_TARGET_DATE, "").let { runCatching { LocalDate.parse(it) }.getOrNull() })
    }
    var showPicker by rememberSaveable { mutableStateOf(false) }

    fun persist(newLabel: String, newDate: LocalDate?) {
        store.edit {
            putString(KEY_LABEL, newLabel)
            putString(KEY_TARGET_DATE, newDate?.toString() ?: "")
        }
    }

    if (showPicker) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        targetDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
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
            Text("  Target date: ${targetDate?.let { formatDate(it) } ?: "select"}")
        }

        val target = targetDate
        if (target != null) {
            val days = ChronoUnit.DAYS.between(LocalDate.now(), target)
            val eventName = label.ifBlank { "the date" }
            val summary = when {
                days > 0 -> "$days day${if (days == 1L) "" else "s"} until $eventName"
                days == 0L -> "$eventName is today!"
                else -> "${-days} day${if (days == -1L) "" else "s"} since $eventName"
            }

            Spacer(Modifier.height(20.dp))
            ResultCard(
                text = summary,
                onCopy = { context.copyToClipboard("Days Until", summary) },
                onShare = { context.shareText(summary) },
            )
        }
    }
}

private fun formatDate(date: LocalDate): String =
    "${date.dayOfMonth}/${date.monthValue}/${date.year}"
