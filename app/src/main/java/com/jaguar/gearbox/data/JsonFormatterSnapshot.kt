package com.jaguar.gearbox.data

/** Inert editor envelope. Syntax is checked only by the bounded JSON engine, never on import. */
data class JsonFormatterSnapshot(val source: String, val mode: String, val indent: Int, val language: String="JSON", val includesLanguage: Boolean=false) {
    fun toInputs(): Map<String, String> = mapOf("source" to source, "mode" to mode, "indent" to indent.toString()) + if(includesLanguage || language!="JSON") mapOf("language" to language) else emptyMap()
}

/** Preserve raw whitespace, escapes and number spelling, including invalid drafts. Storage has
 * separate, deliberately smaller limits; accepting an editor input does not authorize saving it. */
fun parseJsonFormatterSnapshot(values: Map<String, String>): JsonFormatterSnapshot? {
    if (values.keys != setOf("source", "mode", "indent") && values.keys != setOf("source", "mode", "indent", "language")) return null
    if (values.getValue("source").length > 64000 || !validJsonFormatterMode(values - "source")) return null
    return JsonFormatterSnapshot(values.getValue("source"), values.getValue("mode"), values.getValue("indent").toInt(),values["language"] ?: "JSON",values.containsKey("language"))
}

fun validJsonFormatterMode(values: Map<String, String>): Boolean =
    (values.keys == setOf("mode", "indent") || values.keys == setOf("mode", "indent", "language")) &&
        (values["language"] ?: "JSON") in setOf("JSON","XML") &&
        values["mode"] in (if(values["language"]=="XML") setOf("VALIDATE","PRETTY") else setOf("VALIDATE", "PRETTY", "MINIFY")) && values["indent"] in setOf("2", "4")
