package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.jaguar.gearbox.R
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.data.parseTimeZoneSnapshot
import com.jaguar.gearbox.logic.*
import com.jaguar.gearbox.ui.components.*
import java.time.LocalDate

@Composable
fun TimeZoneConverterScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val initialDate = remember { LocalDate.now(java.time.ZoneOffset.UTC).coerceIn(LocalDate.of(1900, 1, 1), LocalDate.of(2100, 12, 31)).toString() }
    var date by rememberSaveable { mutableStateOf(initialDate) }
    var time by rememberSaveable { mutableStateOf("12:00") }
    var sourceZone by rememberSaveable { mutableStateOf("UTC") }
    var targetZone by rememberSaveable { mutableStateOf("UTC") }
    var sourceOffset by rememberSaveable { mutableStateOf("AUTO") }
    var result by remember { mutableStateOf<TimeZoneConversionResult?>(null) }
    var restoreError by remember { mutableStateOf(false) }
    val source = resolveTimeZoneSource(date, time, sourceZone)
    val inputs = mapOf("date" to date, "time" to time, "sourceZone" to sourceZone, "targetZone" to targetZone, "sourceOffset" to sourceOffset)
    fun edited(resetOffset: Boolean = false) { result = null; restoreError = false; if (resetOffset) sourceOffset = "AUTO" }
    fun restore(values: Map<String, String>, completed: Boolean = false) {
        val saved = parseTimeZoneSnapshot(values)
        if (saved == null) { restoreError = true; return }
        date = saved.date; time = saved.time; sourceZone = saved.sourceZone; targetZone = saved.targetZone; sourceOffset = saved.sourceOffset
        edited()
        if (completed) result = convertTimeZone(date, time, sourceZone, targetZone, sourceOffset)
    }
    val output = result?.takeIf { it.error == null }?.let {
        val from = requireNotNull(it.source); val to = requireNotNull(it.target)
        context.getString(R.string.zone_output, conversionLocalText(from), from.zone.id, from.offset.id,
            conversionLocalText(to), to.zone.id, to.offset.id, from.toInstant().toString())
    }
    val history = rememberToolHistory(route = Tools.ROUTE_TIME_ZONE_CONVERTER, inputs = inputs, output = output,
        modeInputs = if (validConversionZone(sourceZone) && validConversionZone(targetZone)) mapOf("sourceZone" to sourceZone, "targetZone" to targetZone) else emptyMap(),
        onRestore = { restore(it) }, onRestoreLastResult = { restore(it, completed = true) },
        onRestoreMode = { values ->
            val from = values["sourceZone"]; val to = values["targetZone"]
            if (from != null && to != null && validConversionZone(from) && validConversionZone(to)) {
                sourceZone = from; targetZone = to; edited(resetOffset = true)
            }
        }, resultRequiresExplicitAction = true, restoredResultRequiresExplicitAction = { false })
    val canConvert = source.error == null && validConversionZone(targetZone) &&
        ((source.offsets.size == 1 && sourceOffset == "AUTO") || (source.offsets.size == 2 && source.offsets.any { it.id == sourceOffset }))
    ToolScaffold(stringResource(R.string.zone_converter_title), Tools.byRoute(Tools.ROUTE_TIME_ZONE_CONVERTER)!!.icon,
        onNavigateBack, history = history) {
        AdaptiveToolLayout(input = {
            OutlinedTextField(date, { date = it; edited(true) }, label = { Text(stringResource(R.string.zone_date)) },
                singleLine = true, isError = parseConversionDate(date) == null,
                supportingText = { if (parseConversionDate(date) == null) Text(stringResource(R.string.zone_date_error)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(time, { time = it; edited(true) }, label = { Text(stringResource(R.string.zone_time)) },
                singleLine = true, isError = parseConversionTime(time) == null,
                supportingText = { if (parseConversionTime(time) == null) Text(stringResource(R.string.zone_time_error)) }, modifier = Modifier.fillMaxWidth())
            ConversionZoneField(sourceZone, { sourceZone = it; edited(true) }, R.string.zone_source)
            ConversionZoneField(targetZone, { targetZone = it; edited() }, R.string.zone_target)
            if (source.error == TimeZoneConversionError.GAP) Text(stringResource(R.string.zone_gap_error), color = MaterialTheme.colorScheme.error)
            if (source.offsets.size == 2) {
                Text(stringResource(R.string.zone_overlap_error))
                CompactChoiceSelector(stringResource(R.string.zone_offset), sourceOffset, listOf("AUTO") + source.offsets.map { it.id },
                    { if (it == "AUTO") context.getString(R.string.zone_choose_offset) else context.getString(R.string.zone_offset_value, it) },
                    { sourceOffset = it; edited() })
            } else if (source.offsets.size == 1) {
                Text(stringResource(R.string.zone_automatic))
                Text(stringResource(R.string.zone_offset_value, source.offsets.single().id))
            }
            Button(enabled = canConvert, onClick = { result = convertTimeZone(date, time, sourceZone, targetZone, sourceOffset); restoreError = false },
                modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.zone_convert)) }
            result?.error?.let { Text(stringResource(when (it) {
                TimeZoneConversionError.INVALID_DATE -> R.string.zone_date_error
                TimeZoneConversionError.INVALID_TIME -> R.string.zone_time_error
                TimeZoneConversionError.INVALID_ZONE -> R.string.zone_id_error
                TimeZoneConversionError.GAP -> R.string.zone_gap_error
                TimeZoneConversionError.OVERLAP_CHOICE_REQUIRED -> R.string.zone_overlap_error
                TimeZoneConversionError.INVALID_OFFSET -> R.string.zone_offset_error
                TimeZoneConversionError.OUT_OF_RANGE -> R.string.zone_range_error
            }), color = MaterialTheme.colorScheme.error) }
            if (restoreError) Text(stringResource(R.string.zone_restore_error), color = MaterialTheme.colorScheme.error)
            Text(stringResource(R.string.zone_rules), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.zone_restore), style = MaterialTheme.typography.bodySmall)
        }, result = {
            Text(stringResource(R.string.zone_result_heading), style = MaterialTheme.typography.titleLarge)
            if (output != null) ResultCard(output) else Text(stringResource(R.string.zone_empty))
            ToolActions(onCopy = { output?.let { context.copyToClipboard(context.getString(R.string.zone_converter_title), it) } },
                onShare = { output?.let { context.shareText(it) } },
                onClear = { date = initialDate; time = "12:00"; edited(true) },
                resultEnabled = output != null, clearEnabled = date != initialDate || time != "12:00" || sourceOffset != "AUTO" || result != null)
        })
    }
}

@Composable
private fun ConversionZoneField(value: String, onChange: (String) -> Unit, label: Int) {
    OutlinedTextField(value, onChange, label = { Text(stringResource(label)) }, singleLine = true,
        isError = !validConversionZone(value), supportingText = { if (!validConversionZone(value)) Text(stringResource(R.string.zone_id_error)) },
        modifier = Modifier.fillMaxWidth())
    if (!validConversionZone(value) && value.isNotBlank()) {
        val suggestions = remember(value) { CONVERSION_ZONE_IDS.filter { it.contains(value, ignoreCase = true) }.take(8) }
        if (suggestions.isNotEmpty()) {
            Text(stringResource(R.string.zone_suggestions), style = MaterialTheme.typography.bodySmall)
            suggestions.forEach { candidate -> TextButton(onClick = { onChange(candidate) }) { Text(candidate) } }
        }
    }
}
