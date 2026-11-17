package com.jaguar.gearbox.ui.components

import androidx.compose.runtime.Composable
import com.jaguar.gearbox.data.validImageStudioMode

/** Reuse the existing preference hydration and clear-generation fences without exposing history.
 * No image, URI, dimensions, crop or generated result is ever passed to the memory helper. */
@Composable
fun rememberImageStudioModes(
    modeInputs: Map<String, String>,
    onRestoreMode: (Map<String, String>) -> Unit,
) {
    rememberToolHistory(
        route = "tool/image_studio",
        inputs = modeInputs.takeIf(::validImageStudioMode) ?: emptyMap(),
        output = null,
        onRestore = {},
        modeInputs = modeInputs.takeIf(::validImageStudioMode) ?: emptyMap(),
        onRestoreMode = { values -> if (validImageStudioMode(values)) onRestoreMode(values) },
        restoreLastResult = false,
        allowRestore = false,
    )
}
