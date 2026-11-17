package com.jaguar.gearbox.data

/** Bounded inert editor inputs. Parsing this envelope never evaluates or executes expression text. */
data class ScientificSnapshot(val expression: String, val angleMode: String) {
    fun toInputs(): Map<String, String> = mapOf("expression" to expression, "angleMode" to angleMode)
}

/** Arithmetic syntax/domain validation remains with the bounded scientific engine on Calculate or
 * explicitly opted-in last-result reconstruction. Raw spelling is preserved to prevent history replay.
 */
fun parseScientificSnapshot(values: Map<String, String>): ScientificSnapshot? {
    if (values.keys != setOf("expression", "angleMode")) return null
    val expression = values.getValue("expression")
    val mode = values.getValue("angleMode")
    if (expression.isBlank() || expression.length > 512 || mode !in setOf("DEG", "RAD")) return null
    return ScientificSnapshot(expression, mode)
}

fun validScientificMode(values: Map<String, String>): Boolean =
    values.keys == setOf("angleMode") && values["angleMode"] in setOf("DEG", "RAD")
