package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.R
import com.jaguar.gearbox.data.*
import com.jaguar.gearbox.logic.*
import com.jaguar.gearbox.ui.components.*

private fun jsonModeLabel(mode: JsonFormatMode): Int = when (mode) {
    JsonFormatMode.VALIDATE -> R.string.json_validate
    JsonFormatMode.PRETTY -> R.string.json_pretty
    JsonFormatMode.MINIFY -> R.string.json_minify
}
@Composable
fun JsonFormatterScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var language by rememberSaveable { mutableStateOf("JSON") }
    var explicitLanguage by rememberSaveable { mutableStateOf(false) }
    var htmlEvaluation by remember { mutableStateOf<HtmlFormatResult?>(null) }
    var xmlEvaluation by remember { mutableStateOf<XmlFormatResult?>(null) }
    var source by rememberSaveable { mutableStateOf("") }
    var modeName by rememberSaveable { mutableStateOf(JsonFormatMode.PRETTY.name) }
    var indent by rememberSaveable { mutableIntStateOf(2) }
    var evaluation by remember { mutableStateOf<JsonFormatResult?>(null) }
    var restoreError by remember { mutableStateOf(false) }
    var shareReview by remember { mutableStateOf<Pair<Map<String, String>, String>?>(null) }
    val mode = JsonFormatMode.valueOf(modeName)
    fun currentInputs() = JsonFormatterSnapshot(source,modeName,indent,language,explicitLanguage).toInputs()
    val inputs = currentInputs()
    fun edited() { evaluation = null; xmlEvaluation = null; htmlEvaluation = null; restoreError = false; shareReview = null }
    fun run() {
        evaluation = null; xmlEvaluation = null; htmlEvaluation = null
        if(language=="HTML") htmlEvaluation=evaluateHtml(source,JsonFormatMode.valueOf(modeName))
        else if(language=="XML") xmlEvaluation=evaluateXml(source,JsonFormatMode.valueOf(modeName),indent)
        else evaluation=evaluateJson(source,JsonFormatMode.valueOf(modeName),indent)
        restoreError=false;shareReview=null
    }
    fun restore(values: Map<String, String>, completed: Boolean = false) {
        val saved = parseJsonFormatterSnapshot(values)
        if (saved == null) { restoreError = true; return }
        source = saved.source; modeName = saved.mode; indent = saved.indent; language=saved.language;explicitLanguage=saved.includesLanguage; edited()
        if (completed) run()
    }
    val successfulOutput = if(language=="HTML") htmlEvaluation?.takeIf {it.error==null}?.output else if(language=="XML") xmlEvaluation?.takeIf {it.error==null}?.output else evaluation?.takeIf {it.error==null}?.output
    val output = successfulOutput?.let { if(mode==JsonFormatMode.VALIDATE) context.getString(if(language=="XML") R.string.xml_valid else R.string.json_valid) else it }
    val history = rememberToolHistory(route = Tools.ROUTE_JSON_FORMATTER, inputs = inputs, output = output,
        modeInputs = inputs - "source",
        restoredModeInputs = {it - "source"},
        onRestore = { restore(it) }, onRestoreLastResult = { restore(it, completed = true) },
        onRestoreMode = { values ->
            if (validJsonFormatterMode(values)) { modeName = values.getValue("mode"); indent = values.getValue("indent").toInt(); language=values["language"] ?: "JSON";explicitLanguage=values.containsKey("language"); edited() }
        }, resultRequiresExplicitAction = true, restoredResultRequiresExplicitAction = { false })
    ToolScaffold(stringResource(R.string.json_title), Tools.byRoute(Tools.ROUTE_JSON_FORMATTER)!!.icon, onNavigateBack, history = history) {
        AdaptiveToolLayout(input = {
            CompactChoiceSelector(stringResource(R.string.code_language),language,listOf("JSON","XML","HTML"),{it},{
                language=it;explicitLanguage=true
                if(language=="HTML") modeName="PRETTY"
                if(language=="XML" && modeName=="MINIFY") modeName="PRETTY"
                edited()
            })
            OutlinedTextField(source, { source = it; edited() }, label = { Text(stringResource(R.string.code_source)) },
                minLines = 5, maxLines = 12, isError = source.length > JSON_INPUT_LIMIT,
                supportingText = { if (source.length > JSON_INPUT_LIMIT) Text(stringResource(R.string.json_error_input_limit)) }, modifier = Modifier.fillMaxWidth())
            CompactChoiceSelector(stringResource(R.string.json_mode), mode, if(language=="HTML") listOf(JsonFormatMode.PRETTY) else if(language=="XML") listOf(JsonFormatMode.VALIDATE,JsonFormatMode.PRETTY) else JsonFormatMode.entries,
                { context.getString(if(language=="HTML") R.string.html_normalize else if(language=="XML" && it==JsonFormatMode.PRETTY) R.string.xml_attributes else jsonModeLabel(it)) }, { modeName = it.name; edited() })
            if (mode == JsonFormatMode.PRETTY && language!="HTML") CompactChoiceSelector(stringResource(R.string.json_indent), indent, listOf(2,4),
                { context.getString(R.string.json_spaces, it) }, { indent = it; edited() })
            Button(enabled = source.length <= JSON_INPUT_LIMIT, onClick = { run() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.json_run)) }
            evaluation?.error?.let { error ->
                val message = context.getString(when (error.kind) {
                    JsonFormatErrorKind.SYNTAX -> R.string.json_error_syntax
                    JsonFormatErrorKind.DUPLICATE_KEY -> R.string.json_error_duplicate
                    JsonFormatErrorKind.INVALID_UNICODE -> R.string.json_error_unicode
                    JsonFormatErrorKind.INPUT_LIMIT -> R.string.json_error_input_limit
                    JsonFormatErrorKind.DEPTH_LIMIT -> R.string.json_error_depth_limit
                    JsonFormatErrorKind.TOKEN_LIMIT -> R.string.json_error_token_limit
                    JsonFormatErrorKind.OUTPUT_LIMIT -> R.string.json_error_output_limit
                })
                Text(stringResource(R.string.json_error_location, message, error.line, error.column), color = MaterialTheme.colorScheme.error)
            }
            xmlEvaluation?.error?.let { error -> Text(stringResource(R.string.xml_error, context.getString(when(error.kind) {
                XmlFormatErrorKind.SYNTAX -> R.string.xml_syntax
                XmlFormatErrorKind.UNSAFE_DECLARATION -> R.string.xml_unsafe
                XmlFormatErrorKind.UNSUPPORTED -> R.string.xml_unsupported
                XmlFormatErrorKind.INPUT_LIMIT -> R.string.json_error_input_limit
                XmlFormatErrorKind.DEPTH_LIMIT -> R.string.json_error_depth_limit
                XmlFormatErrorKind.TOKEN_LIMIT -> R.string.json_error_token_limit
                XmlFormatErrorKind.OUTPUT_LIMIT -> R.string.json_error_output_limit
            }), error.line, error.column),color=MaterialTheme.colorScheme.error) }
            htmlEvaluation?.error?.let { error -> Text(stringResource(when(error) {
                HtmlFormatErrorKind.INPUT_LIMIT -> R.string.json_error_input_limit
                HtmlFormatErrorKind.DEPTH_LIMIT -> R.string.json_error_depth_limit
                HtmlFormatErrorKind.NODE_LIMIT -> R.string.html_node_limit
                HtmlFormatErrorKind.OUTPUT_LIMIT -> R.string.json_error_output_limit
                HtmlFormatErrorKind.UNSUPPORTED -> R.string.html_unsupported
                HtmlFormatErrorKind.PARSE_ERROR -> R.string.html_parse_error
            }),color=MaterialTheme.colorScheme.error) }
            if(language=="HTML") Text(stringResource(R.string.html_rules),style=MaterialTheme.typography.bodySmall)
            if (restoreError) Text(stringResource(R.string.json_restore_error), color = MaterialTheme.colorScheme.error)
            if(language=="XML") Text(stringResource(R.string.xml_rules),style=MaterialTheme.typography.bodySmall)
            if(language=="JSON") Text(stringResource(R.string.json_rules), style = MaterialTheme.typography.bodySmall)
            if(language=="JSON") Text(stringResource(R.string.json_preservation), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.json_limits), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.json_privacy), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.json_retention), style = MaterialTheme.typography.bodySmall)
        }, result = {
            Text(stringResource(R.string.json_result), style = MaterialTheme.typography.titleLarge)
            if (output != null) {
                val lines = remember(output) { output.split('\n') }
                OutlinedCard(Modifier.fillMaxWidth()) {
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 480.dp).padding(12.dp)) {
                        itemsIndexed(lines) { _, line -> Text(line, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)) }
                    }
                }
                if (!ToolMemoryPolicy.validRecord(inputs, output)) Text(stringResource(R.string.json_not_retained), color = MaterialTheme.colorScheme.error)
            } else Text(stringResource(R.string.json_empty))
            ToolActions(onCopy = { output?.let { context.copyToClipboard(context.getString(R.string.json_title), it, sensitive = true) } },
                onShare = { output?.let { shareReview = inputs to it } }, onClear = { source = ""; edited() },
                resultEnabled = output != null, clearEnabled = source.isNotEmpty() || evaluation != null || xmlEvaluation != null || htmlEvaluation != null)
        })
    }
    shareReview?.let { snapshot ->
        if (snapshot.first != inputs || snapshot.second != output) LaunchedEffect(snapshot) { shareReview = null }
        else SensitiveTextShareDialog(snapshot.second, onConfirm = {
            val current = shareReview == snapshot && successfulOutput != null && snapshot.first == currentInputs() && snapshot.second == output
            shareReview = null
            if (current) context.shareText(snapshot.second)
        }, onDismiss = { shareReview = null })
    }
}
