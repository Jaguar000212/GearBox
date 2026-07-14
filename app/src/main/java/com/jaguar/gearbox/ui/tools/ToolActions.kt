package com.jaguar.gearbox.ui.tools

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.PersistableBundle
import android.widget.Toast

/**
 * Copies [text] to the system clipboard and shows a short confirmation toast. Pass
 * [sensitive] = true for secrets (passwords, tokens) so Android 13+ hides the clipboard-preview
 * toast the system would otherwise show.
 */
fun Context.copyToClipboard(label: String, text: String, sensitive: Boolean = false) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    if (sensitive) {
        clip.description.extras = PersistableBundle().apply {
            putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
        }
    }
    clipboard.setPrimaryClip(clip)
    Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
}

/** Opens the system share sheet with [text]. */
fun Context.shareText(text: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    startActivity(Intent.createChooser(sendIntent, null))
}
