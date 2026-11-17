package com.jaguar.gearbox.data

/** Inert editor envelope. Syntax is checked only by the bounded JSON engine, never on import. */
data class JsonFormatterSnapshot(val source: String, val mode: String, val indent: Int) {
    fun toInputs(): Map<String, String> = mapOf("source" to source, "mode" to mode, "indent" to indent.toString())
}

/** Preserve raw whitespace, escapes and number spelling, including invalid drafts. Storage has
 * separate, deliberately smaller limits; accepting an editor input does not authorize saving it. */
fun parseJsonFormatterSnapshot(values: Map<String, String>): JsonFormatterSnapshot? {
    if (values.keys != setOf("source", "mode", "indent")) return null
    if (values.getValue("source").length > 64000 || !validJsonFormatterMode(values - "source")) return null
    return JsonFormatterSnapshot(values.getValue("source"), values.getValue("mode"), values.getValue("indent").toInt())
}

fun validJsonFormatterMode(values: Map<String, String>): Boolean =
    values.keys == setOf("mode", "indent") &&
        values["mode"] in setOf("VALIDATE", "PRETTY", "MINIFY") && values["indent"] in setOf("2", "4")
