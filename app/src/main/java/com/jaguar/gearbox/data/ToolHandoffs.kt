package com.jaguar.gearbox.data

/** Explicit, typed handoffs only. Never infer a number by stripping labels, currencies or units. */
data class ToolHandoff(val label: String, val route: String, val inputs: Map<String, String>, val detail: String)

fun compatibleToolHandoffs(source: String, inputs: Map<String, String>, output: String?): List<ToolHandoff> {
    if (!ToolMemoryPolicy.permitsRoute(source) || output.isNullOrBlank() ||
        !ToolMemoryPolicy.validRecord(inputs, output)) return emptyList()
    val actions = mutableListOf<ToolHandoff>()
    val text = when (source) {
        "tool/text_tools", "tool/hash_generator", "tool/number_to_words" -> output
        "tool/morse_binary" -> output.takeUnless { it.startsWith("Partial translation:") }
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
        "tool/counter" -> inputs["count"]?.toIntOrNull()?.toLong()
        else -> null
    }
    number?.let {
        val value = it.toString()
        actions += ToolHandoff("Number to Words", "tool/number_to_words", mapOf("input" to value), "Use the number $value")
        actions += ToolHandoff("Base Converter", "tool/base_converter", mapOf("input" to value, "fromBase" to "DECIMAL"), "Use $value in decimal")
        actions += ToolHandoff("Decimal to Fraction", "tool/decimal_to_fraction", mapOf("input" to value), "Use the number $value")
        if (it in 1L..3999L) actions += ToolHandoff("Number to Roman", "tool/number_to_roman", mapOf("input" to value), "Convert $value to Roman numerals")
    }
    return actions.filter { it.route != source && ToolMemoryPolicy.validInputs(it.inputs) }
}
