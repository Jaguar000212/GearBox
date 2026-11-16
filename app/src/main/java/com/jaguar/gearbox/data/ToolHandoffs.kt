package com.jaguar.gearbox.data

import com.jaguar.gearbox.logic.decimalToFraction
import java.math.BigDecimal

/** Explicit, typed handoffs only. Never infer a number by stripping labels, currencies or units. */
enum class HandoffMeaning { VALUE, RATIO_QUOTIENT }

data class ToolHandoff(val label: String, val route: String, val inputs: Map<String, String>, val detail: String,
    val meaning: HandoffMeaning = HandoffMeaning.VALUE)

fun compatibleToolHandoffs(source: String, inputs: Map<String, String>, output: String?): List<ToolHandoff> {
    if (!ToolMemoryPolicy.permitsRoute(source) || output.isNullOrBlank() ||
        !ToolMemoryPolicy.validRecord(inputs, output)) return emptyList()
    val actions = mutableListOf<ToolHandoff>()
    val text = when (source) {
        "tool/text_tools", "tool/hash_generator", "tool/number_to_words" -> output
        "tool/morse_binary" -> output.takeUnless { inputs["_partial"] == "true" || it.startsWith("Partial translation:") }
        "tool/qr_generator" -> inputs["text"]
        else -> null
    }
    if (!text.isNullOrBlank()) {
        actions += ToolHandoff("QR Code Generator", "tool/qr_generator", mapOf("text" to text), "Use this text as QR content")
        actions += ToolHandoff("Hash Generator", "tool/hash_generator", mapOf("text" to text, "algorithm" to "SHA256"), "Hash this exact text with SHA-256")
        actions += ToolHandoff("Text Tools", "tool/text_tools", mapOf("text" to text), "Edit or transform this text")
        actions += ToolHandoff("Morse / Binary Translator", "tool/morse_binary", mapOf("input" to text, "mode" to "TEXT_TO_MORSE"), "Translate this text to Morse")
    }
    val number = when (source) {
        "tool/base_converter" -> {
            val radix = when (inputs["fromBase"]) { "BINARY" -> 2; "OCTAL" -> 8; "DECIMAL" -> 10; "HEXADECIMAL" -> 16; else -> 0 }
            if (radix == 0) null else inputs["input"]?.trim()?.toLongOrNull(radix)
        }
        "tool/number_to_words", "tool/number_to_roman" -> inputs["input"]?.trim()?.toLongOrNull()
        "tool/decimal_to_fraction" -> runCatching {
            val raw = inputs["input"]?.trim() ?: return@runCatching null
            // Require a supported source and an exact whole decimal, never round to a Long.
            if (decimalToFraction(raw.toDouble()) == null) null else BigDecimal(raw).longValueExact()
        }.getOrNull()
        "tool/counter" -> inputs["count"]?.toIntOrNull()?.toLong()
        else -> null
    }
    number?.let {
        val value = it.toString()
        actions += ToolHandoff("Number to Words", "tool/number_to_words", mapOf("input" to value), "Use the number $value")
        actions += ToolHandoff("Base Converter", "tool/base_converter", mapOf("input" to value, "fromBase" to "DECIMAL"), "Use $value in decimal")
        if (decimalToFraction(it.toDouble()) != null)
            actions += ToolHandoff("Decimal to Fraction", "tool/decimal_to_fraction", mapOf("input" to value), "Use the number $value")
        if (it in 1L..3999L) actions += ToolHandoff("Number to Roman", "tool/number_to_roman", mapOf("input" to value), "Convert $value to Roman numerals")
    }
    if (source == "tool/ratios") {
        val a = inputs["partA"]?.trim()?.toDoubleOrNull()
        val b = inputs["partB"]?.trim()?.toDoubleOrNull()
        if (a != null && b != null && a.isFinite() && b.isFinite() && b != 0.0) {
            val quotient = a / b
            if (quotient.isFinite() && decimalToFraction(quotient) != null) {
                val value = quotient.toString()
                actions += ToolHandoff("Decimal to Fraction", "tool/decimal_to_fraction", mapOf("input" to value),
                    "Use the ratio quotient $value as a decimal; finite precision applies", HandoffMeaning.RATIO_QUOTIENT)
            }
        }
    }
    return actions.filter { it.route != source && ToolMemoryPolicy.validInputs(it.inputs) }
}
