package com.jaguar.gearbox.data

import com.jaguar.gearbox.logic.TextDiffMode
import com.jaguar.gearbox.logic.validateTextDiffInput

/** Preserve raw inputs and legacy key presence for hydration fingerprints; never normalize text. */
data class TextDiffSnapshot(
    val firstText: String,
    val secondText: String,
    val mode: TextDiffMode = TextDiffMode.EXACT,
    val includesMode: Boolean = false,
) {
    fun toInputs(): Map<String, String> = textDiffInputs(firstText, secondText, mode, includesMode)
}

fun textDiffInputs(firstText: String, secondText: String, mode: TextDiffMode = TextDiffMode.EXACT,
    includesMode: Boolean = false): Map<String, String> =
    mapOf("firstText" to firstText, "secondText" to secondText) +
        if (includesMode || mode != TextDiffMode.EXACT) mapOf("mode" to mode.name) else emptyMap()

/** Calculation bounds are intentionally larger than the separate, unchanged storage limits. */
fun parseTextDiffSnapshot(values: Map<String, String>): TextDiffSnapshot? {
    val includesMode = "mode" in values
    if (values.keys != (setOf("firstText", "secondText") + if (includesMode) setOf("mode") else emptySet())) return null
    val mode = if (includesMode) TextDiffMode.entries.find { it.name == values["mode"] } ?: return null
        else TextDiffMode.EXACT
    val first = values.getValue("firstText")
    val second = values.getValue("secondText")
    if (validateTextDiffInput(first) != null || validateTextDiffInput(second) != null) return null
    return TextDiffSnapshot(first, second, mode, includesMode)
}

fun validTextDiffMode(values: Map<String, String>): Boolean = values.isEmpty() ||
    (values.keys == setOf("mode") && TextDiffMode.entries.any { it.name == values["mode"] })
