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
import com.jaguar.gearbox.logic.*
import com.jaguar.gearbox.ui.components.*

@Composable
fun ScientificCalculatorScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var expression by rememberSaveable { mutableStateOf("") }
    var modeName by rememberSaveable { mutableStateOf(ScientificAngleMode.DEG.name) }
    var value by remember { mutableStateOf<Double?>(null) }
    var error by remember { mutableStateOf<ScientificError?>(null) }
    var restoreError by remember { mutableStateOf(false) }
    val mode = ScientificAngleMode.entries.firstOrNull { it.name == modeName } ?: ScientificAngleMode.DEG
    fun edited() { value = null; error = null; restoreError = false }
    fun calculate() {
        val evaluated = evaluateScientific(expression, ScientificAngleMode.valueOf(modeName))
        value = evaluated.value; error = evaluated.error; restoreError = false
    }
    fun restore(values: Map<String, String>, completed: Boolean = false) {
        val text = values["expression"]
        val selectedMode = ScientificAngleMode.entries.firstOrNull { it.name == values["angleMode"] }
        if (values.keys != setOf("expression", "angleMode") || text == null || text.isBlank() || text.length > 512 || selectedMode == null) {
            restoreError = true; return
        }
        expression = text; modeName = selectedMode.name; edited()
        if (completed) calculate()
    }
    val output = value?.let { context.getString(R.string.scientific_output, expression, modeName, it.toString()) }
    val history = rememberToolHistory(route = Tools.ROUTE_SCIENTIFIC_CALCULATOR,
        inputs = mapOf("expression" to expression, "angleMode" to modeName), output = output,
        modeInputs = mapOf("angleMode" to modeName), onRestore = { restore(it) },
        onRestoreLastResult = { restore(it, completed = true) },
        onRestoreMode = { values -> ScientificAngleMode.entries.firstOrNull { it.name == values["angleMode"] }?.let { modeName = it.name; edited() } },
        resultRequiresExplicitAction = true, restoredResultRequiresExplicitAction = { false })
    ToolScaffold(stringResource(R.string.scientific_title), Tools.byRoute(Tools.ROUTE_SCIENTIFIC_CALCULATOR)!!.icon,
        onNavigateBack, history = history) {
        AdaptiveToolLayout(input = {
            OutlinedTextField(expression, { expression = it; edited() }, label = { Text(stringResource(R.string.scientific_expression)) },
                minLines = 2, maxLines = 6, isError = expression.length > 512,
                supportingText = { if (expression.length > 512) Text(stringResource(R.string.scientific_limit)) },
                modifier = Modifier.fillMaxWidth())
            CompactChoiceSelector(stringResource(R.string.scientific_angle_mode), mode, ScientificAngleMode.entries,
                { context.getString(if (it == ScientificAngleMode.DEG) R.string.scientific_degrees else R.string.scientific_radians) },
                { modeName = it.name; edited() })
            Button(enabled = expression.isNotBlank() && expression.length <= 512, onClick = { calculate() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.scientific_calculate))
            }
            error?.let { Text(stringResource(when (it) {
                ScientificError.SYNTAX -> R.string.scientific_syntax
                ScientificError.LIMIT -> R.string.scientific_limit
                ScientificError.DOMAIN -> R.string.scientific_domain
                ScientificError.DIVISION_BY_ZERO -> R.string.scientific_zero
                ScientificError.NON_FINITE -> R.string.scientific_nonfinite
            }), color = MaterialTheme.colorScheme.error) }
            if (restoreError) Text(stringResource(R.string.scientific_restore_error), color = MaterialTheme.colorScheme.error)
            Text(stringResource(R.string.scientific_operators), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.scientific_functions), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.scientific_numbers), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.scientific_angles), style = MaterialTheme.typography.bodySmall)
        }, result = {
            Text(stringResource(R.string.scientific_result_heading), style = MaterialTheme.typography.titleLarge)
            if (output != null) ResultCard(output) else Text(stringResource(R.string.scientific_empty))
            ToolActions(onCopy = { output?.let { context.copyToClipboard(context.getString(R.string.scientific_title), it) } },
                onShare = { output?.let { context.shareText(it) } }, onClear = { expression = ""; edited() },
                resultEnabled = output != null, clearEnabled = expression.isNotEmpty() || value != null || error != null)
            Text(stringResource(R.string.scientific_precision), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.scientific_retention), style = MaterialTheme.typography.bodySmall)
        })
    }
}
