package com.jaguar.gearbox.logic

import java.security.MessageDigest

enum class TextHashAlgorithm(val label: String, val legacy: Boolean = false) {
    MD5("MD5", true), SHA1("SHA-1", true), SHA256("SHA-256"), SHA512("SHA-512"),
}

/** Hash the exact UTF-8 input; never trim or normalize user text. */
fun textHashHex(input: String, algorithm: TextHashAlgorithm): String =
    MessageDigest.getInstance(algorithm.label).digest(input.toByteArray(Charsets.UTF_8))
        .joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
