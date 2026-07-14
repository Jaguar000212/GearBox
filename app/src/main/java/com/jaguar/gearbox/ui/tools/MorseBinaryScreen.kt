package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.Spacer
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
    BINARY_TO_TEXT("Binary → Text", "Binary (space-separated bytes)"),
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
        selectedMode == TranslateMode.TEXT_TO_MORSE -> textToMorse(input)
        selectedMode == TranslateMode.MORSE_TO_TEXT -> morseToText(input).ifEmpty { null }
        selectedMode == TranslateMode.TEXT_TO_BINARY -> textToBinary(input)
        else -> binaryToText(input)
    }

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

        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text(selectedMode.inputHint) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
            modifier = Modifier.fillMaxWidth(),
        )

        if (output != null) {
            Spacer(Modifier.height(20.dp))
            ResultCard(
                text = output,
                onCopy = { context.copyToClipboard("Translation", output) },
                onShare = { context.shareText(output) },
            )
        } else if (input.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Couldn't translate that input for the selected direction.",
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
