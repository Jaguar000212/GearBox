package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.SensitiveTextShareDialog
import com.jaguar.gearbox.ui.components.ToolHistoryBinding
import com.jaguar.gearbox.ui.components.ToolHistoryDialog
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SensitiveTextReviewTest {
    @get:Rule val compose = createComposeRule()

    @Test fun cancellingReviewNeverConfirmsShare() {
        var shared = 0
        compose.setContent {
            var visible by remember { mutableStateOf(true) }
            MaterialTheme {
                if (visible) SensitiveTextShareDialog("review this payload", { shared++ }, { visible = false })
            }
        }
        compose.onNodeWithText("review this payload").assertExists()
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertEquals(0, shared) }
        compose.onNodeWithText("Review before sharing").assertDoesNotExist()
    }

    @Test fun confirmationIsExplicitAndCalledOnce() {
        var shared = 0
        compose.setContent {
            var visible by remember { mutableStateOf(true) }
            MaterialTheme {
                if (visible) SensitiveTextShareDialog("review this payload", { shared++; visible = false }, { visible = false })
            }
        }
        compose.runOnIdle { assertEquals(0, shared) }
        compose.onNodeWithText("Continue to share").performClick()
        compose.runOnIdle { assertEquals(1, shared) }
    }

    @Test fun manualPrivateSaveWarnsAndPreviewsBeforeSaving() {
        compose.setContent {
            MaterialTheme {
                ToolHistoryDialog(ToolHistoryBinding(Tools.ROUTE_TEXT_TOOLS, {}, mapOf("text" to "sample input"), "SAMPLE OUTPUT"), {})
            }
        }
        compose.onNodeWithText("Keep result").performClick()
        compose.onNodeWithText("Save preview").assertExists()
        compose.onNodeWithText("text: sample input").assertExists()
        compose.onNodeWithText("SAMPLE OUTPUT").assertExists()
        compose.onNodeWithText("Text tools, including Text Diff, QR and Hash, may contain passwords, tokens or private information. This manual save persists even when automatic history and result retention are off. Local storage is not an encrypted vault. Remove secrets before saving.").assertExists()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Save preview").assertDoesNotExist()
    }
}
