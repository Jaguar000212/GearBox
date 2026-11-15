package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.ui.text.font.FontFamily
import com.jaguar.gearbox.logic.hasUnsupportedMorseText
import com.jaguar.gearbox.logic.hasUnknownMorseTokens
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.logic.binaryToText
import com.jaguar.gearbox.logic.morseToText
import com.jaguar.gearbox.logic.textToBinary
import com.jaguar.gearbox.logic.textToMorse
import com.jaguar.gearbox.ui.components.ResultCard
import com.jaguar.gearbox.ui.components.ToolScaffold

private enum class TranslateMode(val label: String, val inputHint: String) {
    TEXT_TO_MORSE("Text → Morse", "Text"),
    MORSE_TO_TEXT("Morse → Text", "Morse (dots/dashes, / between words)"),
    TEXT_TO_BINARY("Text → Binary", "Text"),
    BINARY_TO_TEXT("Binary → Text", "UTF-8 binary bytes"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MorseBinaryScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var mode by rememberSaveable { mutableStateOf(TranslateMode.TEXT_TO_MORSE.name) }
    var modeExpanded by rememberSaveable { mutableStateOf(false) }
    val selectedMode = TranslateMode.valueOf(mode)
    var input by rememberSaveable(selectedMode) { mutableStateOf("") }

    val output = when {
        input.isBlank() -> null
        selectedMode == TranslateMode.TEXT_TO_MORSE -> textToMorse(input).ifBlank { null }
        selectedMode == TranslateMode.MORSE_TO_TEXT -> morseToText(input).ifEmpty { null }
        selectedMode == TranslateMode.TEXT_TO_BINARY -> textToBinary(input)
        else -> binaryToText(input)
    }

    val warning = when {
        selectedMode == TranslateMode.TEXT_TO_MORSE && hasUnsupportedMorseText(input) ->
            "Some characters are not supported by Morse and are omitted from the result."
        selectedMode == TranslateMode.MORSE_TO_TEXT && hasUnknownMorseTokens(input) ->
            "Some Morse tokens are unknown and are omitted. Separate letters with spaces and words with a space, /, and a space."
        else -> null
    }
    val guidance = when (selectedMode) {
        TranslateMode.TEXT_TO_MORSE -> "Example: SOS becomes ... --- ... . Morse output uses uppercase letters."
        TranslateMode.MORSE_TO_TEXT -> "Separate letters with spaces and words with ' / '. Example: .... .. / - .... . .-. ."
        TranslateMode.TEXT_TO_BINARY -> "Text is encoded as UTF-8 bytes. Each byte has eight bits; letters and emoji may use multiple bytes."
        TranslateMode.BINARY_TO_TEXT -> "Enter whitespace-separated UTF-8 bytes, for example 01001000 01101001 for Hi."
    }
    val codeOutput = selectedMode == TranslateMode.TEXT_TO_MORSE || selectedMode == TranslateMode.TEXT_TO_BINARY

    ToolScaffold(
        title = "Morse / Binary Translator",
        icon = Tools.byRoute(Tools.ROUTE_MORSE_BINARY)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        ExposedDropdownMenuBox(
            expanded = modeExpanded,
            onExpandedChange = { modeExpanded = it },
        ) {
            OutlinedTextField(
                value = selectedMode.label,
                onValueChange = {},
                readOnly = true,
                label = { Text("Direction") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modeExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
            )
            DropdownMenu(expanded = modeExpanded, onDismissRequest = { modeExpanded = false }) {
                TranslateMode.entries.forEach { entry ->
                    DropdownMenuItem(
                        text = { Text(entry.label) },
                        onClick = {
                            mode = entry.name
                            modeExpanded = false
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(guidance, style = MaterialTheme.typography.bodyMedium)
        Text("Changing direction clears the input.", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text(selectedMode.inputHint) },
            minLines = 3,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = if (selectedMode == TranslateMode.MORSE_TO_TEXT || selectedMode == TranslateMode.BINARY_TO_TEXT) FontFamily.Monospace else FontFamily.Default,
            ),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
            modifier = Modifier.fillMaxWidth(),
        )

        if (warning != null) {
            Spacer(Modifier.height(8.dp))
            Text(warning, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
        if (output != null) {
            Spacer(Modifier.height(20.dp))
            Text(if (warning == null) "Translation" else "Partial translation", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            ResultCard(
                text = output,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = if (codeOutput) FontFamily.Monospace else FontFamily.Default),
                onCopy = { context.copyToClipboard("Translation", output) },
                onShare = { context.shareText(output) },
            )
        } else if (input.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                if (selectedMode == TranslateMode.BINARY_TO_TEXT) "Enter valid UTF-8 binary bytes. Each token must contain 1–8 zeros or ones, and the byte sequence must form valid UTF-8 text."
                else "No supported characters could be translated.",
                color = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { input = "" }, enabled = input.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Clear, contentDescription = null)
            Text(" Clear")
        }
    }
}
