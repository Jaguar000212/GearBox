package com.jaguar.gearbox

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.jaguar.gearbox.ui.tools.QrGeneratorScreen
import com.jaguar.gearbox.ui.tools.matrixToBitmap
import com.jaguar.gearbox.logic.generateQrMatrix
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class QrGeneratorScreenTest {
    @get:Rule val rule = createComposeRule()

    @Test fun previewAndShareDisappearWhenInputClearedOrInvalid() {
        rule.setContent { MaterialTheme { QrGeneratorScreen {} } }
        rule.onNodeWithText("Your QR code will appear here").assertExists()
        rule.onNodeWithText("Share QR image", substring = true).assertDoesNotExist()
        rule.onNodeWithText("Text or URL").performTextInput("https")
        rule.onNodeWithText("Ready to scan or share").assertExists()
        rule.onNodeWithText("Text or URL").performTextReplacement("x".repeat(5000))
        rule.onNodeWithText("Couldn't generate a QR code. Try shorter text.").assertExists()
        rule.onNodeWithText("Share QR image", substring = true).assertDoesNotExist()
        rule.onNodeWithText("Text or URL").performTextReplacement("")
        rule.onNodeWithText("Your QR code will appear here").assertExists()
    }

    @Test fun sharedPngEncodingPreservesExactPayload() {
        for (payload in listOf("https", "  नमस्ते 世界 👋  ", "abc123 ".repeat(100))) {
            val bitmap = matrixToBitmap(generateQrMatrix(payload)!!)
            val bytes = ByteArrayOutputStream().use { out ->
                assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out))
                out.toByteArray()
            }
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            val pixels = IntArray(decoded.width * decoded.height)
            decoded.getPixels(pixels, 0, decoded.width, 0, 0, decoded.width, decoded.height)
            val result = MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(
                RGBLuminanceSource(decoded.width, decoded.height, pixels)
            )))
            assertEquals(payload, result.text)
        }
    }
}
