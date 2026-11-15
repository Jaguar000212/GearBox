package com.jaguar.gearbox.logic

import java.util.Locale

enum class TextCase(val label: String) {
    UPPER("UPPERCASE"),
    LOWER("lowercase"),
    TITLE("Title Case"),
    CAMEL("camelCase"),
}

fun convertCase(input: String, case: TextCase): String = when (case) {
    TextCase.UPPER -> input.uppercase(Locale.ROOT)
    TextCase.LOWER -> input.lowercase(Locale.ROOT)
    TextCase.TITLE -> input.split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }

    TextCase.CAMEL -> {
        val parts = input.split(Regex("[^A-Za-z0-9]+")).filter { it.isNotEmpty() }
        parts.mapIndexed { index, word ->
            if (index == 0) word.lowercase(Locale.ROOT)
            else word.lowercase(Locale.ROOT).replaceFirstChar { it.titlecase(Locale.ROOT) }
        }.joinToString("")
    }
}
