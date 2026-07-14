package com.jaguar.gearbox.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType

/**
 * The app's standard numeric input: a decimal-keyboard [OutlinedTextField] that only shows
 * [errorText] once the field has been focused and then blurred, instead of on every keystroke.
 * [errorText] is computed by the caller (each tool's validity rules differ - e.g. "must be
 * positive", "must be a whole number") so this only centralizes the keyboard type and *when*
 * the resulting error is allowed to appear on screen.
 */
@Composable
fun DecimalField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    errorText: String? = null,
) {
    var blurredAfterFocus by remember { mutableStateOf(false) }
    val showError = blurredAfterFocus && errorText != null

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        isError = showError,
        supportingText = if (showError) {
            { Text(errorText) }
        } else {
            null
        },
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                if (!focusState.isFocused) blurredAfterFocus = true
            },
    )
}
