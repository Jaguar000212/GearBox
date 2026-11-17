package com.jaguar.gearbox.data

import android.content.Context
import com.jaguar.gearbox.logic.hasUnsupportedMorseText

const val MAX_INCOMING_TEXT_LENGTH = 4096

/** Literal, bounded text only. Routing never starts generation or persists the incoming text. */
enum class IncomingTextDestination(val route: String) {
    TEXT(Tools.ROUTE_TEXT_TOOLS),
    QR(Tools.ROUTE_QR_GENERATOR),
    HASH(Tools.ROUTE_HASH_GENERATOR),
    MORSE(Tools.ROUTE_MORSE_BINARY);

    fun inputs(text: String): Map<String, String>? {
        if (text.isBlank() || text.length > MAX_INCOMING_TEXT_LENGTH) return null
        return when (this) {
            TEXT -> mapOf("text" to text, "case" to "UPPER")
            QR -> mapOf("text" to text)
            HASH -> mapOf("text" to text, "algorithm" to "SHA256")
            MORSE -> mapOf("input" to text, "mode" to "TEXT_TO_MORSE", "_partial" to hasUnsupportedMorseText(text).toString())
        }
    }
}

fun IncomingTextDestination.localizedLabel(context: Context): String =
    requireNotNull(Tools.byRoute(route)).localizedName(context)
