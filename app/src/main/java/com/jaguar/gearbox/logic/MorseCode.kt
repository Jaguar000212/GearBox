package com.jaguar.gearbox.logic

private val MORSE_TABLE: Map<Char, String> = mapOf(
    'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".",
    'F' to "..-.", 'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---",
    'K' to "-.-", 'L' to ".-..", 'M' to "--", 'N' to "-.", 'O' to "---",
    'P' to ".--.", 'Q' to "--.-", 'R' to ".-.", 'S' to "...", 'T' to "-",
    'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-", 'Y' to "-.--",
    'Z' to "--..",
    '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-",
    '5' to ".....", '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----.",
    '.' to ".-.-.-", ',' to "--..--", '?' to "..--..", '\'' to ".----.", '!' to "-.-.--",
    '/' to "-..-.", '(' to "-.--.", ')' to "-.--.-", '&' to ".-...", ':' to "---...",
    ';' to "-.-.-.", '=' to "-...-", '+' to ".-.-.", '-' to "-....-", '_' to "..--.-",
    '"' to ".-..-.", '$' to "...-..-", '@' to ".--.-.",
)

private val MORSE_TO_CHAR: Map<String, Char> = MORSE_TABLE.entries.associate { (k, v) -> v to k }

/** Words are separated by " / " and letters within a word by a single space, per Morse convention. */
fun textToMorse(text: String): String =
    text.uppercase()
        .split(" ")
        .joinToString(" / ") { word ->
            word.mapNotNull { MORSE_TABLE[it] }.joinToString(" ")
        }

fun morseToText(morse: String): String =
    morse.trim()
        .split(" / ")
        .joinToString(" ") { word ->
            word.trim().split(" ").filter { it.isNotEmpty() }
                .mapNotNull { MORSE_TO_CHAR[it] }
                .joinToString("")
        }

fun textToBinary(text: String): String =
    text.map { ch -> ch.code.toString(2).padStart(8, '0') }.joinToString(" ")

/** Returns null if any whitespace-separated token isn't a valid 8-bit binary byte. */
fun binaryToText(binary: String): String? {
    val tokens = binary.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return null
    val chars = tokens.map { token ->
        if (!token.matches(Regex("[01]{1,8}"))) return null
        token.toInt(2).toChar()
    }
    return chars.joinToString("")
}
