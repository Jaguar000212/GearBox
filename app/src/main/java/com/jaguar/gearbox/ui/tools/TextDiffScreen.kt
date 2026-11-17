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
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.data.ToolMemoryPolicy
import com.jaguar.gearbox.data.parseTextDiffSnapshot
import com.jaguar.gearbox.logic.*
import com.jaguar.gearbox.ui.components.*

@Composable
fun TextDiffScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var first by rememberSaveable { mutableStateOf("") }
    var second by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf(TextDiffMode.EXACT) }
    var explicitExactMode by rememberSaveable { mutableStateOf(false) }
    var comparison by remember { mutableStateOf<TextDiffResult?>(null) }
    var restoreError by remember { mutableStateOf(false) }
    var shareReview by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    val inputs = mapOf("firstText" to first, "secondText" to second) +
        if (mode != TextDiffMode.EXACT || explicitExactMode) mapOf("mode" to mode.name) else emptyMap()
    val firstError = validateTextDiffInput(first)
    val secondError = validateTextDiffInput(second)
    fun edited() { comparison = null; restoreError = false; shareReview = null }
    fun restore(values: Map<String, String>, completed: Boolean = false) {
        val saved = parseTextDiffSnapshot(values)
        if (saved == null) { restoreError = true; return }
        first = saved.firstText; second = saved.secondText
        mode = saved.mode; explicitExactMode = "mode" in values; edited()
        if (completed) comparison = compareTextLines(first, second, mode).takeIf { it.error == null }
    }
    val rows = comparison?.lines.orEmpty()
    val counts = comparison?.let { context.getString(R.string.text_diff_counts, it.added, it.removed, it.equal) }
    fun rowText(line: TextDiffLine): String {
        val prefix = when (line.kind) { TextDiffKind.ADDED -> "+"; TextDiffKind.REMOVED -> "−"; TextDiffKind.EQUAL -> "=" }
        val label = context.getString(when (line.kind) {
            TextDiffKind.ADDED -> R.string.text_diff_added
            TextDiffKind.REMOVED -> R.string.text_diff_removed
            TextDiffKind.EQUAL -> R.string.text_diff_equal
        })
        if (line.ignoredWhitespace) return context.getString(R.string.text_diff_equivalent_line,
            line.text.ifEmpty { context.getString(R.string.text_diff_empty_line) },
            line.secondText.orEmpty().ifEmpty { context.getString(R.string.text_diff_empty_line) })
        return "$prefix " + context.getString(R.string.text_diff_line, label, line.text.ifEmpty { context.getString(R.string.text_diff_empty_line) })
    }
    val output = if (counts != null) context.getString(R.string.text_diff_export, counts,
        context.getString(if (mode == TextDiffMode.EXACT) R.string.text_diff_exact_export else R.string.text_diff_whitespace_export) + "\n" + context.getString(R.string.text_diff_legend), rows.joinToString("\n", transform = ::rowText)) else null
    val history = rememberToolHistory(route = Tools.ROUTE_TEXT_DIFF, inputs = inputs, output = output,
        onRestore = { restore(it) },
        modeInputs = if (mode == TextDiffMode.EXACT && !explicitExactMode) emptyMap() else mapOf("mode" to mode.name),
        onRestoreMode = { saved ->
            val restored = saved["mode"]?.let { runCatching { TextDiffMode.valueOf(it) }.getOrNull() }
            if (saved.isEmpty()) {
                mode = TextDiffMode.EXACT; explicitExactMode = false; edited()
            } else if (restored != null && saved.keys == setOf("mode")) {
                mode = restored; explicitExactMode = true; edited()
            }
        },
        restoredModeInputs = { saved -> saved["mode"]?.let { mapOf("mode" to it) }.orEmpty() },
        onRestoreLastResult = { restore(it, completed = true) },
        resultRequiresExplicitAction = true, restoredResultRequiresExplicitAction = { false })
    ToolScaffold(stringResource(R.string.text_diff_title), Tools.byRoute(Tools.ROUTE_TEXT_DIFF)!!.icon,
        onNavigateBack, history = history) {
        AdaptiveToolLayout(input = {
            CompactChoiceSelector(label = stringResource(R.string.text_diff_mode), selected = mode,
                choices = TextDiffMode.entries, choiceLabel = { context.getString(if (it == TextDiffMode.EXACT)
                    R.string.text_diff_exact else R.string.text_diff_ignore_ascii) },
                onSelect = { if (mode != it) { mode = it; explicitExactMode = true; edited() } })
            DiffInput(first, { first = it; edited() }, R.string.text_diff_first, firstError)
            DiffInput(second, { second = it; edited() }, R.string.text_diff_second, secondError)
            Button(enabled = firstError == null && secondError == null,
                onClick = { comparison = compareTextLines(first, second, mode).takeIf { it.error == null }; restoreError = false },
                modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.text_diff_compare)) }
            if (restoreError) Text(stringResource(R.string.text_diff_restore_error), color = MaterialTheme.colorScheme.error)
            Text(stringResource(R.string.text_diff_rules), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.text_diff_privacy), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.text_diff_retention), style = MaterialTheme.typography.bodySmall)
        }, result = {
            Text(stringResource(R.string.text_diff_result), style = MaterialTheme.typography.titleLarge)
            if (output == null) Text(stringResource(R.string.text_diff_empty)) else {
                Text(requireNotNull(counts), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.text_diff_legend), style = MaterialTheme.typography.bodySmall)
                OutlinedCard(Modifier.fillMaxWidth()) {
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 480.dp).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(rows) { _, line -> Text(rowText(line), style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)) }
                    }
                }
                if (!ToolMemoryPolicy.validRecord(inputs, output)) Text(stringResource(R.string.text_diff_not_retained), color = MaterialTheme.colorScheme.error)
            }
            ToolActions(onCopy = { output?.let { context.copyToClipboard(context.getString(R.string.text_diff_title), it, sensitive = true) } },
                onShare = { output?.let { shareReview = Triple(first, second, it) } },
                onClear = { first = ""; second = ""; edited() },
                resultEnabled = output != null, clearEnabled = first.isNotEmpty() || second.isNotEmpty() || comparison != null)
        })
    }
    shareReview?.let { snapshot ->
        if (snapshot.first != first || snapshot.second != second || snapshot.third != output) {
            LaunchedEffect(snapshot) { shareReview = null }
        } else SensitiveTextShareDialog(snapshot.third, onConfirm = {
            val stillCurrent = shareReview == snapshot && comparison != null &&
                snapshot.first == first && snapshot.second == second && snapshot.third == output
            shareReview = null
            if (stillCurrent) context.shareText(snapshot.third)
        }, onDismiss = { shareReview = null })
    }
}

@Composable
private fun DiffInput(value: String, onChange: (String) -> Unit, label: Int, error: TextDiffError?) {
    OutlinedTextField(value, onChange, label = { Text(stringResource(label)) }, minLines = 3, maxLines = 8,
        isError = error != null, supportingText = { error?.let { Text(stringResource(
            if (it == TextDiffError.TOO_LONG) R.string.text_diff_too_long else R.string.text_diff_too_many_lines)) } },
        modifier = Modifier.fillMaxWidth())
}
