package com.jaguar.gearbox.data

import com.jaguar.gearbox.logic.validDecisionLabel

/** Compact, bounded input-only representation; no content belongs in remembered modes. */
data class DecisionMatrixSnapshot(
    val optionLabels: List<String>,
    val criterionLabels: List<String>,
    val weights: List<Int>,
    val scores: List<List<Int>>,
) {
    fun toInputs(): Map<String, String> = mapOf(
        "optionLabels" to optionLabels.joinToString("\n"),
        "criterionLabels" to criterionLabels.joinToString("\n"),
        "weights" to weights.joinToString(","),
        "scores" to scores.joinToString("\n") { it.joinToString(",") },
    )
}

/** No escaping/JSON parser needed: labels are single-line, numeric separators are unambiguous.
 * Canonical numeric tokens plus untouched label text preserve exact hydration fingerprints.
 */
fun parseDecisionMatrixSnapshot(values: Map<String, String>): DecisionMatrixSnapshot? {
    if (values.keys != setOf("optionLabels", "criterionLabels", "weights", "scores") ||
        !ToolMemoryPolicy.validInputs(values)) return null
    val options = values.getValue("optionLabels").split('\n')
    val criteria = values.getValue("criterionLabels").split('\n')
    if (options.size !in 2..6 || criteria.size !in 1..6) return null
    if ((options + criteria).any { !validDecisionLabel(it) }) return null
    val weightTokens = values.getValue("weights").split(',')
    if (weightTokens.size != criteria.size || weightTokens.any { !Regex("0|[1-9][0-9]?|100").matches(it) }) return null
    val weights = weightTokens.map(String::toInt)
    if (weights.none { it > 0 }) return null
    val rows = values.getValue("scores").split('\n')
    if (rows.size != options.size) return null
    val scores = rows.map { row ->
        val tokens = row.split(',')
        if (tokens.size != criteria.size || tokens.any { !Regex("[0-5]").matches(it) }) return null
        tokens.map(String::toInt)
    }
    return DecisionMatrixSnapshot(options, criteria, weights, scores)
}
