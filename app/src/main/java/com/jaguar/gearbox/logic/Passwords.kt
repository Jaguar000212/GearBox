package com.jaguar.gearbox.logic

import java.security.SecureRandom
import kotlin.math.ln

/** Generate with every enabled class represented, then shuffle with the same secure RNG. */
fun generatePassword(
    length: Int,
    enabledClasses: List<String>,
    random: SecureRandom = SecureRandom(),
): String {
    require(enabledClasses.isNotEmpty() && enabledClasses.all { it.isNotEmpty() })
    require(length >= enabledClasses.size) { "Length must fit every enabled character class." }
    val charset = enabledClasses.joinToString("")
    val chars = enabledClasses.map { it[random.nextInt(it.length)] }.toMutableList()
    repeat(length - chars.size) { chars += charset[random.nextInt(charset.length)] }
    for (i in chars.lastIndex downTo 1) {
        val j = random.nextInt(i + 1)
        val old = chars[i]
        chars[i] = chars[j]
        chars[j] = old
    }
    return chars.joinToString("")
}

/** Estimate using the generated output's original settings, not the current form controls. */
fun passwordStrength(length: Int, generatedCharsetSize: Int): Pair<String, Float> {
    if (length <= 0 || generatedCharsetSize <= 1) return "Unavailable" to 0f
    val bits = length * ln(generatedCharsetSize.toDouble()) / ln(2.0)
    return when {
        bits < 40 -> "Weak" to 0.25f
        bits < 60 -> "Medium" to 0.5f
        bits < 80 -> "Strong" to 0.75f
        else -> "Very strong" to 1f
    }
}
