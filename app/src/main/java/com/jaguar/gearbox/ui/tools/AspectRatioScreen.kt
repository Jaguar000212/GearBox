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
import com.jaguar.gearbox.R
import com.jaguar.gearbox.data.*
import com.jaguar.gearbox.logic.*
import com.jaguar.gearbox.ui.components.*
import java.math.BigDecimal

private fun sizingModeLabel(mode: AspectSizingMode): Int = when (mode) {
    AspectSizingMode.RATIO -> R.string.aspect_ratio_mode
    AspectSizingMode.WIDTH -> R.string.aspect_width_mode
    AspectSizingMode.HEIGHT -> R.string.aspect_height_mode
    AspectSizingMode.PERCENT -> R.string.aspect_percent_mode
    AspectSizingMode.FIT -> R.string.aspect_fit_mode
    AspectSizingMode.FILL -> R.string.aspect_fill_mode
}

@Composable
fun AspectRatioScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var modeName by rememberSaveable { mutableStateOf(AspectSizingMode.RATIO.name) }
    var sourceWidth by rememberSaveable { mutableStateOf("") }
    var sourceHeight by rememberSaveable { mutableStateOf("") }
    var targetWidth by rememberSaveable { mutableStateOf("") }
    var targetHeight by rememberSaveable { mutableStateOf("") }
    var percent by rememberSaveable { mutableStateOf("100") }
    var result by remember { mutableStateOf<AspectSizingResult?>(null) }
    var restoreError by remember { mutableStateOf(false) }
    val mode = AspectSizingMode.valueOf(modeName)
    val inputs = aspectRatioInputs(modeName, sourceWidth, sourceHeight, targetWidth, targetHeight, percent)
    val setup = parseAspectRatioSnapshot(inputs)
    fun edited() { result = null; restoreError = false }
    fun calculate() {
        val active = parseAspectRatioSnapshot(aspectRatioInputs(modeName, sourceWidth, sourceHeight, targetWidth, targetHeight, percent)) ?: return
        result = calculateAspectSizing(requireNotNull(parsePixelDimension(active.sourceWidth)), requireNotNull(parsePixelDimension(active.sourceHeight)),
            parsePixelDimension(active.targetWidth) ?: 0, parsePixelDimension(active.targetHeight) ?: 0,
            parseSizingPercent(active.percent) ?: BigDecimal("100"), AspectSizingMode.valueOf(active.mode))
        restoreError = false
    }
    fun restore(values: Map<String, String>, completed: Boolean = false) {
        val saved = parseAspectRatioSnapshot(values)
        if (saved == null) { restoreError = true; return }
        modeName = saved.mode; sourceWidth = saved.sourceWidth; sourceHeight = saved.sourceHeight
        targetWidth = saved.targetWidth; targetHeight = saved.targetHeight; percent = saved.percent
        edited(); if (completed) calculate()
    }
    val output = result?.takeIf { it.error == null }?.let { value ->
        val main = if (mode == AspectSizingMode.RATIO) context.getString(R.string.aspect_ratio_output,
            value.ratioWidth, value.ratioHeight, value.width, value.height) else context.getString(R.string.aspect_output,
            value.ratioWidth, value.ratioHeight, value.width, value.height, value.scalePercent(), value.scaleNumerator, value.scaleDenominator)
        if (mode == AspectSizingMode.FIT || mode == AspectSizingMode.FILL) {
            val horizontal = value.horizontalAdjustment; val vertical = value.verticalAdjustment
            main + "\n" + context.getString(if (mode == AspectSizingMode.FIT) R.string.aspect_padding else R.string.aspect_crop,
                targetWidth.toInt(), targetHeight.toInt(), horizontal / 2, horizontal - horizontal / 2, vertical / 2, vertical - vertical / 2)
        } else main
    }
    val history = rememberToolHistory(route = Tools.ROUTE_ASPECT_RATIO, inputs = inputs, output = output,
        modeInputs = mapOf("mode" to modeName), onRestore = { restore(it) }, onRestoreLastResult = { restore(it, completed = true) },
        onRestoreMode = { values -> AspectSizingMode.entries.firstOrNull { it.name == values["mode"] }?.let { modeName = it.name; edited() } },
        resultRequiresExplicitAction = true, restoredResultRequiresExplicitAction = { false })
    ToolScaffold(stringResource(R.string.aspect_title), Tools.byRoute(Tools.ROUTE_ASPECT_RATIO)!!.icon, onNavigateBack, history = history) {
        AdaptiveToolLayout(input = {
            CompactChoiceSelector(stringResource(R.string.aspect_mode), mode, AspectSizingMode.entries,
                { context.getString(sizingModeLabel(it)) }, { modeName = it.name; edited() })
            PixelField(sourceWidth, { sourceWidth = it; edited() }, R.string.aspect_source_width)
            PixelField(sourceHeight, { sourceHeight = it; edited() }, R.string.aspect_source_height)
            if (mode in setOf(AspectSizingMode.WIDTH, AspectSizingMode.FIT, AspectSizingMode.FILL))
                PixelField(targetWidth, { targetWidth = it; edited() }, R.string.aspect_target_width)
            if (mode in setOf(AspectSizingMode.HEIGHT, AspectSizingMode.FIT, AspectSizingMode.FILL))
                PixelField(targetHeight, { targetHeight = it; edited() }, R.string.aspect_target_height)
            if (mode == AspectSizingMode.PERCENT) OutlinedTextField(percent, { percent = it; edited() },
                label = { Text(stringResource(R.string.aspect_percent)) }, singleLine = true, isError = parseSizingPercent(percent) == null,
                supportingText = { if (parseSizingPercent(percent) == null) Text(stringResource(R.string.aspect_percent_error)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            Button(enabled = setup != null, onClick = { calculate() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.aspect_calculate)) }
            if (result?.error != null) Text(stringResource(R.string.aspect_range_error), color = MaterialTheme.colorScheme.error)
            if (restoreError) Text(stringResource(R.string.aspect_restore_error), color = MaterialTheme.colorScheme.error)
            Text(stringResource(R.string.aspect_setup_note), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.aspect_rounding), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.aspect_no_edit), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.aspect_restore), style = MaterialTheme.typography.bodySmall)
        }, result = {
            Text(stringResource(R.string.aspect_result_title), style = MaterialTheme.typography.titleLarge)
            if (output != null) ResultCard(output) else Text(stringResource(R.string.aspect_empty))
            ToolActions(onCopy = { output?.let { context.copyToClipboard(context.getString(R.string.aspect_title), it) } },
                onShare = { output?.let { context.shareText(it) } },
                onClear = { sourceWidth = ""; sourceHeight = ""; targetWidth = ""; targetHeight = ""; percent = "100"; edited() },
                resultEnabled = output != null, clearEnabled = sourceWidth.isNotEmpty() || sourceHeight.isNotEmpty() || targetWidth.isNotEmpty() || targetHeight.isNotEmpty() || percent != "100" || result != null)
        })
    }
}

@Composable
private fun PixelField(value: String, onChange: (String) -> Unit, label: Int) {
    OutlinedTextField(value, onChange, label = { Text(stringResource(label)) }, singleLine = true, isError = parsePixelDimension(value) == null,
        supportingText = { if (parsePixelDimension(value) == null) Text(stringResource(R.string.aspect_dimension_error)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
}
