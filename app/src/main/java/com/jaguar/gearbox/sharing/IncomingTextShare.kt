package com.jaguar.gearbox.sharing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import android.content.Intent
import com.jaguar.gearbox.data.MAX_INCOMING_TEXT_LENGTH
import java.util.UUID

/** Transient preview only. Never put this object or its text into saved instance state. */
data class IncomingTextShare(val text: String?, val id: String = UUID.randomUUID().toString())

/** Literal plain text only. Never dereferences a URI, reads a stream, parses HTML or executes text. */
fun parseIncomingTextShare(intent: Intent): IncomingTextShare? {
    if (intent.action != Intent.ACTION_SEND && intent.action != Intent.ACTION_SEND_MULTIPLE) return null
    return runCatching {
        if (intent.action != Intent.ACTION_SEND || intent.type != "text/plain" || intent.data != null ||
            intent.hasExtra(Intent.EXTRA_STREAM) || intent.hasExtra(Intent.EXTRA_HTML_TEXT)) return@runCatching IncomingTextShare(null)
        val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)
        if (text == null || text.length !in 1..MAX_INCOMING_TEXT_LENGTH || text.isBlank()) return@runCatching IncomingTextShare(null)
        val plain = text.toString()
        intent.clipData?.let { clip ->
            if (clip.itemCount != 1) return@runCatching IncomingTextShare(null)
            val item = clip.getItemAt(0)
            if (item.uri != null || item.intent != null || item.htmlText != null ||
                item.text?.toString() != plain) return@runCatching IncomingTextShare(null)
        }
        IncomingTextShare(plain)
    }.getOrElse { IncomingTextShare(null) }
}

/** Main-thread, Activity-owned claim boundary. An old dialog cannot claim a newer share. */
class IncomingTextInbox {
    var pending: IncomingTextShare? by mutableStateOf(null)
        private set
    fun replace(value: IncomingTextShare?) { pending = value }
    fun claim(id: String): Boolean {
        if (pending?.id != id) return false
        pending = null
        return true
    }
}
