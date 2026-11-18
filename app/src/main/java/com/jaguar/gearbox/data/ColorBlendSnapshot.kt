package com.jaguar.gearbox.data

import com.jaguar.gearbox.logic.colorBlendHex
import com.jaguar.gearbox.logic.parseOpaqueHex

/** Named palette setup / completed-run inputs only. Restoring it never performs a blend. */
data class ColorBlendSnapshot(val firstHex: String, val secondHex: String, val weight: Int) {
    fun toInputs(): Map<String, String> = mapOf("firstHex" to firstHex, "secondHex" to secondHex, "weight" to weight.toString())
}

/** Fail closed on unsupported schemas, alpha, partial hex, non-integer percentages or extra fields. */
fun parseColorBlendSnapshot(values: Map<String, String>): ColorBlendSnapshot? {
    if (values.keys != setOf("firstHex", "secondHex", "weight")) return null
    val hex = Regex("#[0-9a-fA-F]{6}")
    val firstText = values["firstHex"] ?: return null
    val secondText = values["secondHex"] ?: return null
    if (!hex.matches(firstText) || !hex.matches(secondText)) return null
    val weightText = values["weight"] ?: return null
    if (!Regex("0|[1-9][0-9]?|100").matches(weightText)) return null
    val first = parseOpaqueHex(firstText) ?: return null
    val second = parseOpaqueHex(secondText) ?: return null
    return ColorBlendSnapshot(colorBlendHex(first), colorBlendHex(second), weightText.toInt())
}
