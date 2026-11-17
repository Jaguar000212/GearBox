package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.R
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.data.parseBusinessDaySnapshot
import com.jaguar.gearbox.logic.*
import com.jaguar.gearbox.ui.components.*
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BusinessDaysScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val today = remember { LocalDate.now().coerceIn(BUSINESS_DATE_MIN, BUSINESS_DATE_MAX).toString() }
    var mode by rememberSaveable { mutableStateOf("ADD") }
    var start by rememberSaveable { mutableStateOf(today) }
    var end by rememberSaveable { mutableStateOf(today) }
    var offset by rememberSaveable { mutableStateOf("1") }
    var mask by rememberSaveable { mutableIntStateOf(96) }
    var exclusions by rememberSaveable { mutableStateOf("") }
    var resultDate by remember { mutableStateOf<LocalDate?>(null) }
    var resultCount by remember { mutableStateOf<Int?>(null) }
    var rangeError by remember { mutableStateOf(false) }
    var restoreError by remember { mutableStateOf(false) }
    val inputs = mapOf("mode" to mode, "start" to start, "end" to end, "offset" to offset,
        "weekendMask" to mask.toString(), "excludedDates" to exclusions)
    val setup = parseBusinessDaySnapshot(inputs)
    fun edited() { resultDate = null; resultCount = null; rangeError = false; restoreError = false }
    fun calculate() {
        edited()
        if (parseBusinessDaySnapshot(mapOf("mode" to mode, "start" to start, "end" to end, "offset" to offset,
                "weekendMask" to mask.toString(), "excludedDates" to exclusions)) == null) return
        try {
            val startDate = requireNotNull(parseBusinessDate(start))
            val omitted = requireNotNull(parseBusinessExclusions(exclusions))
            if (mode == "ADD") resultDate = businessDateOffset(startDate, requireNotNull(parseBusinessOffset(offset)), mask, omitted)
            else resultCount = countBusinessDates(startDate, requireNotNull(parseBusinessDate(end)), mask, omitted)
        } catch (_: IllegalArgumentException) { rangeError = true }
    }
    fun restore(values: Map<String, String>, completed: Boolean = false) {
        val saved = parseBusinessDaySnapshot(values)
        if (saved == null) { restoreError = true; return }
        mode = saved.mode; start = saved.start; end = saved.end
        offset = values.getValue("offset"); mask = saved.weekendMask; exclusions = saved.excludedDates
        edited()
        if (completed) calculate()
    }
    val output = resultDate?.let { context.getString(R.string.business_result_date, it.toString(), start, offset.toInt()) }
        ?: resultCount?.let { context.getString(R.string.business_result_count, it, start, end) }
    val history = rememberToolHistory(route = Tools.ROUTE_BUSINESS_DAY_PLANNER, inputs = inputs, output = output,
        modeInputs = if (mask in 0..126) mapOf("mode" to mode, "weekendMask" to mask.toString()) else emptyMap(),
        onRestore = { restore(it) }, onRestoreLastResult = { restore(it, completed = true) },
        onRestoreMode = { values ->
            // Search can select a mode only; presets supply both keys. Preserve other user inputs.
            val requested = values["mode"]
            val requestedMask = values["weekendMask"]?.let(::parseBusinessWeekendMask)
            if (requested in listOf("ADD", "COUNT") && (!values.containsKey("weekendMask") || requestedMask != null)) {
                mode = requireNotNull(requested); if (requestedMask != null) mask = requestedMask; edited()
            }
        }, resultRequiresExplicitAction = true, restoredResultRequiresExplicitAction = { false })
    ToolScaffold(stringResource(R.string.business_title), Tools.byRoute(Tools.ROUTE_BUSINESS_DAY_PLANNER)!!.icon,
        onNavigateBack, history = history) {
        AdaptiveToolLayout(input = {
            CompactChoiceSelector(stringResource(R.string.business_mode), mode, listOf("ADD", "COUNT"),
                { context.getString(if (it == "ADD") R.string.business_add else R.string.business_count) },
                { mode = it; edited() })
            BusinessField(start, { start = it; edited() }, R.string.business_start, parseBusinessDate(start) == null, R.string.business_date_error)
            BusinessField(end, { end = it; edited() }, R.string.business_end, parseBusinessDate(end) == null, R.string.business_date_error)
            BusinessField(offset, { offset = it; edited() }, R.string.business_offset, parseBusinessOffset(offset) == null, R.string.business_offset_error)
            Text(stringResource(R.string.business_setup_note), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.business_weekends), style = MaterialTheme.typography.titleMedium)
            val days = listOf(R.string.business_monday, R.string.business_tuesday, R.string.business_wednesday,
                R.string.business_thursday, R.string.business_friday, R.string.business_saturday, R.string.business_sunday)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                days.forEachIndexed { index, name ->
                    FilterChip(selected = mask and (1 shl index) != 0,
                        onClick = { mask = mask xor (1 shl index); edited() },
                        label = { Text(stringResource(name)) })
                }
            }
            Text(stringResource(R.string.business_weekend_note), style = MaterialTheme.typography.bodySmall)
            if (mask == 127) Text(stringResource(R.string.business_all_weekdays_error), color = MaterialTheme.colorScheme.error)
            OutlinedTextField(exclusions, { exclusions = it; edited() }, label = { Text(stringResource(R.string.business_exclusions)) },
                minLines = 2, maxLines = 6, isError = parseBusinessExclusions(exclusions) == null,
                supportingText = { if (parseBusinessExclusions(exclusions) == null) Text(stringResource(R.string.business_exclusions_error)) },
                modifier = Modifier.fillMaxWidth())
            Text(stringResource(R.string.business_exclusions_note), style = MaterialTheme.typography.bodySmall)
            Button(enabled = setup != null, onClick = { calculate() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.business_calculate)) }
            if (rangeError) Text(stringResource(R.string.business_range_error), color = MaterialTheme.colorScheme.error)
            if (restoreError) Text(stringResource(R.string.business_restore_error), color = MaterialTheme.colorScheme.error)
            Text(stringResource(R.string.business_rules), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.business_calendar_note), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.business_restore_note), style = MaterialTheme.typography.bodySmall)
        }, result = {
            Text(stringResource(R.string.business_result_heading), style = MaterialTheme.typography.titleLarge)
            if (output != null) ResultCard(output) else Text(stringResource(R.string.business_empty))
            ToolActions(onCopy = { output?.let { context.copyToClipboard(context.getString(R.string.business_title), it) } },
                onShare = { output?.let { context.shareText(it) } },
                onClear = { start = today; end = today; offset = "0"; exclusions = ""; edited() },
                resultEnabled = output != null,
                clearEnabled = start != today || end != today || offset != "0" || exclusions.isNotEmpty() || output != null)
        })
    }
}

@Composable
private fun BusinessField(value: String, onChange: (String) -> Unit, label: Int, invalid: Boolean, error: Int) {
    OutlinedTextField(value, onChange, label = { Text(stringResource(label)) }, singleLine = true,
        isError = invalid, supportingText = { if (invalid) Text(stringResource(error)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
}
