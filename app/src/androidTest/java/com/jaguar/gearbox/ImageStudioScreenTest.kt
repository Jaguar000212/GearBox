package com.jaguar.gearbox

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.images.ImageStudioImage
import com.jaguar.gearbox.ui.tools.ImageStudioScreen
import org.junit.After
import org.junit.Rule
import org.junit.Test
import java.io.File

class ImageStudioScreenTest {
    @get:Rule val compose = createComposeRule()
    private val isolated = IsolatedToolTestContext()
    private val context = object : ContextWrapper(isolated) {
        override fun getApplicationContext(): Context = this
        override fun getCacheDir(): File = File(isolated.noBackupFilesDir, "test-cache").also { it.mkdirs() }
    }
    @After fun cleanup() { isolated.cleanup() }
    @Test fun pickingStateDoesNotGenerateAndEditsInvalidateEncodedPreview() {
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.RED)
        compose.setContent { CompositionLocalProvider(LocalContext provides context) {
            MaterialTheme { ImageStudioScreen({}, ImageStudioImage(bitmap, 8, 8)) }
        } }
        compose.onNodeWithContentDescription(context.getString(R.string.studio_output_preview)).assertDoesNotExist()
        compose.onNodeWithText(context.getString(R.string.studio_generate)).performScrollTo().performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithContentDescription(context.getString(R.string.studio_output_preview)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(context.getString(R.string.studio_width)).performScrollTo().performTextReplacement("4")
        compose.onNodeWithContentDescription(context.getString(R.string.studio_output_preview)).assertDoesNotExist()
        compose.onNodeWithText(context.getString(R.string.studio_right)).performScrollTo().performTextReplacement("0")
        compose.onNodeWithText(context.getString(R.string.studio_generate)).performScrollTo().assertIsNotEnabled()
    }
    @Test fun exportReviewCanBeCancelledWithoutLaunchingDestinationOrShare() {
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        compose.setContent { CompositionLocalProvider(LocalContext provides context) {
            MaterialTheme { ImageStudioScreen({}, ImageStudioImage(bitmap, 8, 8)) }
        } }
        compose.onNodeWithText(context.getString(R.string.studio_generate)).performScrollTo().performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText(context.getString(R.string.studio_share)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(context.getString(R.string.studio_share)).performScrollTo().performClick()
        compose.onNodeWithText(context.getString(R.string.studio_share_note)).assertExists()
        compose.onNodeWithText(context.getString(R.string.studio_cancel)).performClick()
        compose.onNodeWithText(context.getString(R.string.studio_share_note)).assertDoesNotExist()
        compose.onNodeWithText(context.getString(R.string.studio_save)).performScrollTo().performClick()
        compose.onNodeWithText(context.getString(R.string.studio_cancel)).performClick()
        compose.onNodeWithText(context.getString(R.string.studio_saved)).assertDoesNotExist()
    }
    @Test fun markupRequiresExplicitAddAndChangesInvalidateGeneratedOutput() {
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        compose.setContent { CompositionLocalProvider(LocalContext provides context) {
            MaterialTheme { ImageStudioScreen({}, ImageStudioImage(bitmap, 8, 8)) }
        } }
        compose.onNodeWithText(context.getString(R.string.studio_markup_show)).performScrollTo().performClick()
        compose.onNodeWithText(context.getString(R.string.studio_markup_count, 0)).assertExists()
        compose.onNodeWithText(context.getString(R.string.studio_markup_add)).performScrollTo().performClick()
        compose.onNodeWithText(context.getString(R.string.studio_markup_count, 1)).assertExists()
        compose.onNodeWithText(context.getString(R.string.studio_generate)).performScrollTo().performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithContentDescription(context.getString(R.string.studio_output_preview)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(context.getString(R.string.studio_markup_delete, 1)).performScrollTo().performClick()
        compose.onNodeWithContentDescription(context.getString(R.string.studio_output_preview)).assertDoesNotExist()
        compose.onNodeWithText(context.getString(R.string.studio_markup_count, 0)).assertExists()
        compose.onNodeWithText(context.getString(R.string.studio_markup_color)).performScrollTo().performTextReplacement("#12345678")
        compose.onNodeWithText(context.getString(R.string.studio_markup_add)).performScrollTo().assertIsNotEnabled()
    }

}
