package com.jaguar.gearbox.data

import com.jaguar.gearbox.logic.validateTextDiffInput

/** Inert, bounded editor input. Preserve raw line endings for hydration fingerprints. */
data class TextDiffSnapshot(val firstText: String, val secondText: String) {
    fun toInputs(): Map<String, String> = mapOf("firstText" to firstText, "secondText" to secondText)
}

/** Calculation bounds are intentionally larger than the separate, unchanged storage limits. */
fun parseTextDiffSnapshot(values: Map<String, String>): TextDiffSnapshot? {
    if (values.keys != setOf("firstText", "secondText")) return null
    val first = values.getValue("firstText")
    val second = values.getValue("secondText")
    if (validateTextDiffInput(first) != null || validateTextDiffInput(second) != null) return null
    return TextDiffSnapshot(first, second)
}
