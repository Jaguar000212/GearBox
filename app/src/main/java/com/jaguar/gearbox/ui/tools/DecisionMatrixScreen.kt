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
import com.jaguar.gearbox.data.ToolMemoryPolicy
import com.jaguar.gearbox.data.parseDecisionMatrixSnapshot
import com.jaguar.gearbox.logic.*
import com.jaguar.gearbox.ui.components.*

private data class MatrixRemoval(val criterion: Boolean, val index: Int)
private fun replaced(values: ArrayList<String>, index: Int, value: String) = ArrayList(values).also { it[index] = value }

@Composable
fun DecisionMatrixScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var labels by rememberSaveable { mutableStateOf(ArrayList((1..3).map { context.getString(R.string.matrix_default_option, it) })) }
    var criteria by rememberSaveable { mutableStateOf(ArrayList((1..3).map { context.getString(R.string.matrix_default_criterion, it) })) }
    var weights by rememberSaveable { mutableStateOf(arrayListOf("1", "1", "1")) }
    var ratings by rememberSaveable { mutableStateOf(ArrayList(List(9) { "0" })) }
    var result by remember { mutableStateOf<DecisionMatrixResult?>(null) }
    var restoreError by remember { mutableStateOf(false) }
    var removal by remember { mutableStateOf<MatrixRemoval?>(null) }
    var shareReview by remember { mutableStateOf<Pair<Map<String, String>, String>?>(null) }
    val inputs = mapOf("optionLabels" to labels.joinToString("\n"), "criterionLabels" to criteria.joinToString("\n"),
        "weights" to weights.joinToString(","), "scores" to ratings.chunked(criteria.size).joinToString("\n") { it.joinToString(",") })
    val setup = parseDecisionMatrixSnapshot(inputs)
    fun edited() { result = null; restoreError = false; removal = null; shareReview = null }
    fun calculate() {
        val valid = parseDecisionMatrixSnapshot(mapOf("optionLabels" to labels.joinToString("\n"), "criterionLabels" to criteria.joinToString("\n"),
            "weights" to weights.joinToString(","), "scores" to ratings.chunked(criteria.size).joinToString("\n") { it.joinToString(",") })) ?: return
        result = evaluateDecisionMatrix(valid.optionLabels.mapIndexed { index, label -> DecisionOption(label, valid.scores[index]) },
            valid.criterionLabels.mapIndexed { index, label -> DecisionCriterion(label, valid.weights[index]) })
        restoreError = false
    }
    fun restore(values: Map<String, String>, completed: Boolean = false) {
        val saved = parseDecisionMatrixSnapshot(values)
        if (saved == null) { restoreError = true; return }
        labels = ArrayList(saved.optionLabels); criteria = ArrayList(saved.criterionLabels)
        weights = ArrayList(saved.weights.map { it.toString() }); ratings = ArrayList(saved.scores.flatten().map { it.toString() })
        edited(); if (completed) calculate()
    }
    val output = result?.let { calculated ->
        val lines = calculated.scores.map { score ->
            val row = context.getString(R.string.matrix_score, score.rank, score.optionIndex + 1, labels[score.optionIndex],
                score.displayMean(), score.numerator, score.denominator)
            if (calculated.scores.count { it.rank == score.rank } > 1) row + "\n" + context.getString(R.string.matrix_tie) else row
        }.joinToString("\n\n")
        context.getString(R.string.matrix_export, context.getString(R.string.matrix_total_weight, calculated.totalWeight), lines,
            context.getString(R.string.matrix_precision))
    }
    val history = rememberToolHistory(route = Tools.ROUTE_DECISION_MATRIX, inputs = inputs, output = output,
        onRestore = { restore(it) }, onRestoreLastResult = { restore(it, completed = true) },
        resultRequiresExplicitAction = true, restoredResultRequiresExplicitAction = { false })
    ToolScaffold(stringResource(R.string.matrix_title), Tools.byRoute(Tools.ROUTE_DECISION_MATRIX)!!.icon,
        onNavigateBack, history = history) {
        AdaptiveToolLayout(input = {
            Text(stringResource(R.string.matrix_criteria), style = MaterialTheme.typography.titleLarge)
            criteria.indices.forEach { index ->
                OutlinedCard(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        MatrixField(criteria[index], { criteria = replaced(criteria, index, it); edited() },
                            stringResource(R.string.matrix_criterion_label, index + 1), numeric = false)
                        MatrixField(weights[index], { weights = replaced(weights, index, it); edited() },
                            stringResource(R.string.matrix_weight, index + 1), numeric = true)
                        TextButton(enabled = criteria.size > 1, onClick = { removal = MatrixRemoval(true, index) }) {
                            Text(stringResource(R.string.matrix_remove_criterion, index + 1))
                        }
                    }
                }
            }
            OutlinedButton(enabled = criteria.size < 6, onClick = {
                val oldCount = criteria.size
                ratings = ArrayList(ratings.chunked(oldCount).flatMap { it + "0" })
                criteria = ArrayList(criteria + context.getString(R.string.matrix_default_criterion, oldCount + 1))
                weights = ArrayList(weights + "1"); edited()
            }) { Text(stringResource(R.string.matrix_add_criterion)) }
            Text(stringResource(R.string.matrix_options), style = MaterialTheme.typography.titleLarge)
            labels.indices.forEach { option ->
                OutlinedCard(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        MatrixField(labels[option], { labels = replaced(labels, option, it); edited() },
                            stringResource(R.string.matrix_option_label, option + 1), numeric = false)
                        criteria.indices.forEach { criterion ->
                            val index = option * criteria.size + criterion
                            MatrixField(ratings[index], { ratings = replaced(ratings, index, it); edited() },
                                stringResource(R.string.matrix_rating, option + 1, criterion + 1, criteria[criterion]), numeric = true)
                        }
                        TextButton(enabled = labels.size > 2, onClick = { removal = MatrixRemoval(false, option) }) {
                            Text(stringResource(R.string.matrix_remove_option, option + 1))
                        }
                    }
                }
            }
            OutlinedButton(enabled = labels.size < 6, onClick = {
                labels = ArrayList(labels + context.getString(R.string.matrix_default_option, labels.size + 1))
                ratings = ArrayList(ratings + List(criteria.size) { "0" }); edited()
            }) { Text(stringResource(R.string.matrix_add_option)) }
            if (setup == null) Text(stringResource(R.string.matrix_invalid), color = MaterialTheme.colorScheme.error)
            Button(enabled = setup != null, onClick = { calculate() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.matrix_compare)) }
            if (restoreError) Text(stringResource(R.string.matrix_restore_error), color = MaterialTheme.colorScheme.error)
            Text(stringResource(R.string.matrix_rules), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.matrix_advisory), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.matrix_privacy), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.matrix_retention), style = MaterialTheme.typography.bodySmall)
        }, result = {
            Text(stringResource(R.string.matrix_result_title), style = MaterialTheme.typography.titleLarge)
            if (output != null) {
                ResultCard(output, selectable = false)
                if (!ToolMemoryPolicy.validRecord(inputs, output)) Text(stringResource(R.string.matrix_not_retained), color = MaterialTheme.colorScheme.error)
            } else Text(stringResource(R.string.matrix_empty))
            ToolActions(onCopy = { output?.let { context.copyToClipboard(context.getString(R.string.matrix_title), it, sensitive = true) } },
                onShare = { output?.let { shareReview = inputs to it } }, resultEnabled = output != null)
        })
    }
    removal?.let { request ->
        AlertDialog(onDismissRequest = { removal = null }, title = { Text(stringResource(R.string.matrix_remove_title)) },
            text = { Text(stringResource(R.string.matrix_remove_note)) },
            confirmButton = { TextButton(onClick = {
                if (removal === request) {
                    if (request.criterion && criteria.size > 1 && request.index in criteria.indices) {
                        ratings = ArrayList(ratings.chunked(criteria.size).flatMap { row -> row.filterIndexed { index, _ -> index != request.index } })
                        criteria = ArrayList(criteria.filterIndexed { index, _ -> index != request.index })
                        weights = ArrayList(weights.filterIndexed { index, _ -> index != request.index })
                    } else if (!request.criterion && labels.size > 2 && request.index in labels.indices) {
                        ratings = ArrayList(ratings.chunked(criteria.size).filterIndexed { index, _ -> index != request.index }.flatten())
                        labels = ArrayList(labels.filterIndexed { index, _ -> index != request.index })
                    }
                    edited()
                }
            }) { Text(stringResource(R.string.matrix_remove)) } },
            dismissButton = { TextButton(onClick = { removal = null }) { Text(stringResource(R.string.matrix_cancel)) } })
    }
    shareReview?.let { snapshot ->
        if (snapshot.first != inputs || snapshot.second != output) LaunchedEffect(snapshot) { shareReview = null }
        else SensitiveTextShareDialog(snapshot.second, onConfirm = {
            val current = shareReview == snapshot && result != null && snapshot.first == inputs && snapshot.second == output
            shareReview = null
            if (current) context.shareText(snapshot.second)
        }, onDismiss = { shareReview = null })
    }
}

@Composable
private fun MatrixField(value: String, onChange: (String) -> Unit, label: String, numeric: Boolean) {
    OutlinedTextField(value, onChange, label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text), modifier = Modifier.fillMaxWidth())
}
