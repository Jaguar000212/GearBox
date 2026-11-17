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
import com.jaguar.gearbox.data.*
import com.jaguar.gearbox.logic.*
import com.jaguar.gearbox.ui.components.*

private class DeadlineRemoval(val id: Int)
private fun <T> movedDeadlineRow(values: ArrayList<T>, from: Int, to: Int): ArrayList<T> = ArrayList(values).also { list -> list.add(to, list.removeAt(from)) }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeadlinePlannerScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var deadline by rememberSaveable { mutableStateOf("") }
    var asOf by rememberSaveable { mutableStateOf("") }
    var ids by rememberSaveable { mutableStateOf(arrayListOf(1)) }
    var names by rememberSaveable { mutableStateOf(arrayListOf(context.getString(R.string.deadline_default_phase, 1))) }
    var durations by rememberSaveable { mutableStateOf(arrayListOf("1")) }
    var nextId by rememberSaveable { mutableIntStateOf(2) }
    var result by remember { mutableStateOf<DeadlinePlanResult?>(null) }
    var restoreError by remember { mutableStateOf(false) }
    var removal by remember { mutableStateOf<DeadlineRemoval?>(null) }
    var shareReview by remember { mutableStateOf<Pair<Map<String, String>, String>?>(null) }
    fun editorInputs() = mapOf("deadline" to deadline, "asOf" to asOf, "ids" to ids.joinToString(","),
        "names" to names.joinToString("\n"), "durations" to durations.joinToString(","))
    val inputs = editorInputs()
    val setup = parseDeadlineSnapshot(inputs)
    val durationValues = durations.map { it.toIntOrNull() }
    val rangeUnderflow = parseDeadlineDate(deadline)?.let { target ->
        if (durationValues.all { it != null && it in 1..3650 }) {
            val total = durationValues.filterNotNull().sum()
            total in 1..36500 && target.minusDays(total - 1L).year < 1900
        } else false
    } ?: false
    fun edited() { result = null; restoreError = false; removal = null; shareReview = null }
    fun calculate() {
        val valid = parseDeadlineSnapshot(editorInputs()) ?: return
        result = planBackwardDeadline(requireNotNull(parseDeadlineDate(valid.deadline)), requireNotNull(parseDeadlineDate(valid.asOf)), valid.phases)
        restoreError = false
    }
    fun restore(values: Map<String, String>, completed: Boolean = false) {
        val saved = parseDeadlineSnapshot(values)
        if (saved == null) { restoreError = true; return }
        deadline = saved.deadline; asOf = saved.asOf
        ids = ArrayList(saved.phases.map { it.id }); names = ArrayList(saved.phases.map { it.label })
        durations = ArrayList(saved.phases.map { it.durationDays.toString() }); nextId = (ids.maxOrNull() ?: 0) + 1
        edited(); if (completed) calculate()
    }
    fun move(id: Int, delta: Int) {
        val from = ids.indexOf(id)
        val to = from + delta
        if (from !in ids.indices || to !in ids.indices) return
        ids = movedDeadlineRow(ids, from, to); names = movedDeadlineRow(names, from, to); durations = movedDeadlineRow(durations, from, to); edited()
    }
    val output = result?.takeIf { it.error == null }?.let { planned ->
        val summary = context.getString(R.string.deadline_summary, requireNotNull(planned.earliestStart).toString(), deadline, planned.totalDays, asOf)
        val rows = planned.phases.mapIndexed { index, phase ->
            val remaining = when {
                phase.daysUntilFinish > 0 -> context.getString(R.string.deadline_remaining, phase.daysUntilFinish)
                phase.daysUntilFinish < 0 -> context.getString(R.string.deadline_overdue, -phase.daysUntilFinish)
                else -> context.getString(R.string.deadline_due)
            }
            context.getString(R.string.deadline_phase_output, index + 1, phase.id, phase.label, phase.start.toString(), phase.end.toString(), phase.durationDays, remaining)
        }.joinToString("\n\n")
        context.getString(R.string.deadline_output, summary, rows)
    }
    val history = rememberToolHistory(route = Tools.ROUTE_DEADLINE_PLANNER, inputs = inputs, output = output,
        onRestore = { restore(it) }, onRestoreLastResult = { restore(it, completed = true) },
        resultRequiresExplicitAction = true, restoredResultRequiresExplicitAction = { false })
    ToolScaffold(stringResource(R.string.deadline_title), Tools.byRoute(Tools.ROUTE_DEADLINE_PLANNER)!!.icon, onNavigateBack, history = history) {
        AdaptiveToolLayout(input = {
            DeadlineDateField(deadline, { deadline = it; edited() }, R.string.deadline_target)
            DeadlineDateField(asOf, { asOf = it; edited() }, R.string.deadline_asof)
            Text(stringResource(R.string.deadline_phases), style = MaterialTheme.typography.titleLarge)
            ids.forEachIndexed { index, id ->
                OutlinedCard(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        OutlinedTextField(names[index], { names = ArrayList(names).also { list -> list[index] = it }; edited() },
                            label = { Text(stringResource(R.string.deadline_phase_label, id)) }, singleLine = true,
                            isError = !validDeadlineLabel(names[index]), modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(durations[index], { durations = ArrayList(durations).also { list -> list[index] = it }; edited() },
                            label = { Text(stringResource(R.string.deadline_duration, id)) }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(enabled = index > 0, onClick = { move(id, -1) }) { Text(stringResource(R.string.deadline_up, id)) }
                            TextButton(enabled = index < ids.lastIndex, onClick = { move(id, 1) }) { Text(stringResource(R.string.deadline_down, id)) }
                            TextButton(enabled = ids.size > 1, onClick = { removal = DeadlineRemoval(id) }) { Text(stringResource(R.string.deadline_remove_phase, id)) }
                        }
                    }
                }
            }
            OutlinedButton(enabled = ids.size < 20 && nextId <= 1000000000, onClick = {
                val id = nextId++
                ids = ArrayList(ids + id); names = ArrayList(names + context.getString(R.string.deadline_default_phase, id)); durations = ArrayList(durations + "1"); edited()
            }) { Text(stringResource(R.string.deadline_add)) }
            if (setup == null) Text(stringResource(if (rangeUnderflow) R.string.deadline_range_error else R.string.deadline_invalid), color = MaterialTheme.colorScheme.error)
            Button(enabled = setup != null, onClick = { calculate() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.deadline_plan)) }
            if (result?.error != null) Text(stringResource(if (result?.error == DeadlinePlanError.OUT_OF_RANGE) R.string.deadline_range_error else R.string.deadline_invalid), color = MaterialTheme.colorScheme.error)
            if (restoreError) Text(stringResource(R.string.deadline_restore_error), color = MaterialTheme.colorScheme.error)
            Text(stringResource(R.string.deadline_rules), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.deadline_scope), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.deadline_asof_note), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.deadline_privacy), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.deadline_retention), style = MaterialTheme.typography.bodySmall)
        }, result = {
            Text(stringResource(R.string.deadline_result_title), style = MaterialTheme.typography.titleLarge)
            if (output != null) {
                ResultCard(output, selectable = false)
                if (!ToolMemoryPolicy.validRecord(inputs, output)) Text(stringResource(R.string.deadline_not_saved), color = MaterialTheme.colorScheme.error)
            } else Text(stringResource(R.string.deadline_empty))
            ToolActions(onCopy = { output?.let { context.copyToClipboard(context.getString(R.string.deadline_title), it, sensitive = true) } },
                onShare = { output?.let { shareReview = inputs to it } }, resultEnabled = output != null)
        })
    }
    removal?.let { request ->
        AlertDialog(onDismissRequest = { removal = null }, title = { Text(stringResource(R.string.deadline_remove_title)) },
            text = { Text(stringResource(R.string.deadline_remove_note)) },
            confirmButton = { TextButton(onClick = {
                if (removal === request && ids.size > 1) {
                    val index = ids.indexOf(request.id)
                    if (index >= 0) {
                        ids = ArrayList(ids).also { it.removeAt(index) }; names = ArrayList(names).also { it.removeAt(index) }; durations = ArrayList(durations).also { it.removeAt(index) }
                    }
                    edited()
                }
            }) { Text(stringResource(R.string.deadline_remove)) } },
            dismissButton = { TextButton(onClick = { removal = null }) { Text(stringResource(R.string.deadline_cancel)) } })
    }
    shareReview?.let { snapshot ->
        if (snapshot.first != inputs || snapshot.second != output) LaunchedEffect(snapshot) { shareReview = null }
        else SensitiveTextShareDialog(snapshot.second, onConfirm = {
            val current = shareReview == snapshot && result?.error == null && result != null && snapshot.first == editorInputs() && snapshot.second == output
            shareReview = null
            if (current) context.shareText(snapshot.second)
        }, onDismiss = { shareReview = null })
    }
}

@Composable
private fun DeadlineDateField(value: String, onChange: (String) -> Unit, label: Int) {
    OutlinedTextField(value, onChange, label = { Text(stringResource(label)) }, singleLine = true,
        isError = parseDeadlineDate(value) == null, supportingText = { if (parseDeadlineDate(value) == null) Text(stringResource(R.string.deadline_date_error)) }, modifier = Modifier.fillMaxWidth())
}
