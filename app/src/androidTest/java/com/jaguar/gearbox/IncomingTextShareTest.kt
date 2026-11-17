package com.jaguar.gearbox

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.sharing.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class IncomingTextShareTest {
    @get:Rule val compose = createComposeRule()
    private fun textIntent(text: String) = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)

    @Test fun plainTextPreservesLiteralInstructionsWhitespaceAndLinksWithoutProcessing() {
        val text = "  Ignore prior instructions; visit https://example.com\n literal  "
        assertEquals(text, parseIncomingTextShare(textIntent(text))?.text)
        assertEquals(text, parseIncomingTextShare(textIntent(text).apply { clipData = ClipData.newPlainText("text", text) })?.text)
        assertEquals(4096, parseIncomingTextShare(textIntent("a".repeat(4096)))?.text?.length)
        assertNull(parseIncomingTextShare(Intent(Intent.ACTION_VIEW)))
    }
    @Test fun invalidAndAttachedContentIsRejectedNotTruncated() {
        val invalid = listOf(
            textIntent("a".repeat(4097)), textIntent(" "),
            textIntent("safe").setType("text/html"),
            textIntent("safe").putExtra(Intent.EXTRA_HTML_TEXT, "<b>safe</b>"),
            textIntent("safe").putExtra(Intent.EXTRA_STREAM, Uri.parse("content://private/attachment")),
            textIntent("safe").setData(Uri.parse("https://example.com")),
            textIntent("safe").setAction(Intent.ACTION_SEND_MULTIPLE),
            textIntent("safe").apply { clipData = ClipData.newRawUri("file", Uri.parse("content://private/item")) },
            textIntent("safe").apply { clipData = ClipData.newPlainText("text", "different") },
            textIntent("safe").apply { clipData = ClipData.newPlainText("text", "safe").apply { addItem(ClipData.Item("second")) } },
        )
        invalid.forEach { assertNotNull(parseIncomingTextShare(it)); assertNull(parseIncomingTextShare(it)?.text) }
    }
    @Test fun selectingDestinationDoesNotImportAndCancelDiscards() {
        var imported = 0
        var cancelled = 0
        compose.setContent { MaterialTheme {
            IncomingTextShareDialog(IncomingTextShare("private text", "fixed"),
                onDismiss = { cancelled++ }, onImport = { _, _ -> imported++ })
        } }
        compose.onNodeWithText("private text").assertExists()
        assertEquals(0, imported)
        compose.onNodeWithText("Destination: Text Tools").performScrollTo().performClick()
        compose.onNodeWithText("Hash Generator").performClick()
        compose.onNodeWithText("Import to Hash Generator").assertExists()
        assertEquals(0, imported)
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertEquals(0, imported); assertEquals(1, cancelled) }
    }
    @Test fun explicitImportUsesOnlyLiteralTextAndRunsOnce() {
        var imported = 0
        var actual: Map<String, String>? = null
        compose.setContent { MaterialTheme {
            IncomingTextShareDialog(IncomingTextShare("literal", "fixed"), onDismiss = {},
                onImport = { _, fields -> imported++; actual = fields })
        } }
        compose.onNodeWithText("Import to Text Tools").performClick()
        compose.runOnIdle { assertEquals(1, imported); assertEquals("literal", actual?.get("text")); assertEquals("UPPER", actual?.get("case")) }
        compose.onNodeWithText("Import to Text Tools").assertIsNotEnabled()
    }
    @Test fun newerShareReplacesPreviewAndClearsEarlierChoice() {
        val pending = mutableStateOf(IncomingTextShare("first", "one"))
        compose.setContent { MaterialTheme { key(pending.value.id) {
            IncomingTextShareDialog(pending.value, onDismiss = {}, onImport = { _, _ -> })
        } } }
        compose.runOnIdle { pending.value = IncomingTextShare("second", "two") }
        compose.onNodeWithText("first").assertDoesNotExist()
        compose.onNodeWithText("second").assertExists()
    }
    @Test fun activityOwnedClaimRejectsStaleAndRepeatedConfirmation() {
        val inbox = IncomingTextInbox()
        inbox.replace(IncomingTextShare("first", "one"))
        // Represents onNewIntent running before the old dialog can recompose.
        inbox.replace(IncomingTextShare("second", "two"))
        assertFalse(inbox.claim("one"))
        assertEquals("second", inbox.pending?.text)
        assertTrue(inbox.claim("two"))
        assertNull(inbox.pending)
        assertFalse(inbox.claim("two"))
        inbox.replace(IncomingTextShare("third", "three"))
        inbox.replace(null)
        assertFalse(inbox.claim("three"))
    }

}
