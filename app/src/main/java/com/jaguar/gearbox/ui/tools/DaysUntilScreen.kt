package com.jaguar.gearbox.ui.tools

import androidx.compose.ui.res.stringResource
import com.jaguar.gearbox.R
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
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.ResultCard
import com.jaguar.gearbox.ui.components.AdaptiveToolLayout
import com.jaguar.gearbox.ui.components.ToolActions
import com.jaguar.gearbox.ui.components.ToolScaffold
import com.jaguar.gearbox.ui.components.rememberToolHistory
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import com.jaguar.gearbox.ui.components.rememberCurrentLocalDate


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaysUntilScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val today by rememberCurrentLocalDate()
    // Existing legacy values remain untouched; automatic restore now follows Tool memory settings.
    var label by rememberSaveable { mutableStateOf("") }
    var targetDate by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    var showPicker by rememberSaveable { mutableStateOf(false) }

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

    val history = rememberToolHistory(
        route = Tools.ROUTE_DAYS_UNTIL,
        inputs = mapOf("label" to label, "targetDate" to (targetDate?.toString() ?: "")),
        output = targetDate?.let { localizedDaysUntilSummary(context, today, it, label) },
        onRestore = { saved ->
            label = saved["label"] ?: ""
            targetDate = saved["targetDate"]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        },
    )

    ToolScaffold(
        title = context.getString(R.string.calc_days_until),
        icon = Tools.byRoute(Tools.ROUTE_DAYS_UNTIL)!!.icon,
        onNavigateBack = onNavigateBack,
        history = history,
    ) {
        AdaptiveToolLayout(input = {
            Text(stringResource(R.string.calc_count_calendar_days_to_a_date_or_days_since_it_passed),
                style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = label,
                onValueChange = {
                    label = it
                },
                label = { Text(stringResource(R.string.calc_event_name_optional)) },
                placeholder = { Text(stringResource(R.string.calc_e_g_diwali)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.DateRange, contentDescription = null)
                Text(context.getString(R.string.calc_target_date_1_s, targetDate?.let { formatDate(it) } ?: context.getString(R.string.calc_choose_date)))
            }
        }, result = {
            val target = targetDate
            if (target != null) {
                val summary = localizedDaysUntilSummary(context, today, target, label)

                Spacer(Modifier.height(20.dp))
                ResultCard(
                    text = summary,
                    textStyle = MaterialTheme.typography.headlineMedium,
                    onCopy = { context.copyToClipboard(context.getString(R.string.calc_days_until), localizedDaysUntilSummary(context, LocalDate.now(), target, label)) },
                    onShare = { context.shareText(localizedDaysUntilSummary(context, LocalDate.now(), target, label)) },
                )
                Spacer(Modifier.height(8.dp))
                Text(context.getString(R.string.calc_from_1_s_to_2_s, formatDate(today), formatDate(target)),
                    style = MaterialTheme.typography.bodyMedium)
            } else {
                Spacer(Modifier.height(20.dp))
                Text(stringResource(R.string.calc_choose_a_target_date_to_see_your_countdown_an_event_nam),
                    style = MaterialTheme.typography.bodyMedium)
            }
            ToolActions(onClear = { label = ""; targetDate = null; showPicker = false },
                clearEnabled = label.isNotEmpty() || targetDate != null)
        })
    }
}

private fun formatDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

internal fun localizedDaysUntilSummary(context: android.content.Context, today: LocalDate, target: LocalDate, label: String): String {
    val days = java.time.temporal.ChronoUnit.DAYS.between(today, target)
    val event = label.ifBlank { context.getString(R.string.calc_default_event) }
    if (days == 0L) return context.getString(R.string.calc_event_today, event)
    val count = kotlin.math.abs(days)
    return context.resources.getQuantityString(
        if (days > 0) R.plurals.calc_days_until else R.plurals.calc_days_since,
        count.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), count, event)
}
