package com.jaguar.gearbox.logic

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

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

/** UTF-8 bytes, one eight-bit token per byte. ASCII output is unchanged. */
fun textToBinary(text: String): String =
    text.toByteArray(Charsets.UTF_8)
        .joinToString(" ") { byte -> (byte.toInt() and 0xff).toString(2).padStart(8, '0') }

/** Accepts whitespace-separated 1–8 bit bytes; rejects malformed UTF-8 instead of replacing it. */
fun binaryToText(binary: String): String? {
    val tokens = binary.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return null
    val bytes = ByteArray(tokens.size)
    tokens.forEachIndexed { index, token ->
        if (!token.matches(Regex("[01]{1,8}"))) return null
        bytes[index] = token.toInt(2).toByte()
    }
    return try {
        Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes)).toString()
    } catch (_: CharacterCodingException) {
        null
    }
}

/** These warnings mirror the existing permissive Morse conversion without changing its output. */
fun hasUnsupportedMorseText(text: String): Boolean =
    text.uppercase().any { it != ' ' && it !in MORSE_TABLE }

fun hasUnknownMorseTokens(morse: String): Boolean =
    morse.trim().split(" / ").any { word ->
        word.trim().split(" ").any { it.isNotEmpty() && it !in MORSE_TO_CHAR }
    }
