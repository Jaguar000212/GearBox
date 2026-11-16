package com.jaguar.gearbox.ui.tools

import com.jaguar.gearbox.R
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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.jaguar.gearbox.ui.components.CompactChoiceSelector
import com.jaguar.gearbox.ui.components.rememberToolHistory

private enum class TranslateMode(val labelRes: Int, val inputHintRes: Int) {
    TEXT_TO_MORSE(R.string.special_text_morse, R.string.hash_input),
    MORSE_TO_TEXT(R.string.special_morse_text, R.string.special_morse_dots_dashes_between_words),
    TEXT_TO_BINARY(R.string.special_text_binary, R.string.hash_input),
    BINARY_TO_TEXT(R.string.special_binary_text, R.string.special_utf_8_binary_bytes),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MorseBinaryScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var mode by rememberSaveable { mutableStateOf(TranslateMode.TEXT_TO_MORSE.name) }
    val selectedMode = TranslateMode.valueOf(mode)
    var input by rememberSaveable { mutableStateOf("") }

    val output = when {
        input.isBlank() -> null
        selectedMode == TranslateMode.TEXT_TO_MORSE -> textToMorse(input).ifBlank { null }
        selectedMode == TranslateMode.MORSE_TO_TEXT -> morseToText(input).ifEmpty { null }
        selectedMode == TranslateMode.TEXT_TO_BINARY -> textToBinary(input)
        else -> binaryToText(input)
    }

    val warning = when {
        selectedMode == TranslateMode.TEXT_TO_MORSE && hasUnsupportedMorseText(input) ->
            context.getString(R.string.special_some_characters_are_not_supported_by_morse_and_are_omitted_from_t)
        selectedMode == TranslateMode.MORSE_TO_TEXT && hasUnknownMorseTokens(input) ->
            context.getString(R.string.special_some_morse_tokens_are_unknown_and_are_omitted_separate_letters_wi)
        else -> null
    }
    val guidance = when (selectedMode) {
        TranslateMode.TEXT_TO_MORSE -> context.getString(R.string.special_example_sos_becomes_morse_output_uses_uppercase_letters)
        TranslateMode.MORSE_TO_TEXT -> context.getString(R.string.special_separate_letters_with_spaces_and_words_with_example)
        TranslateMode.TEXT_TO_BINARY -> context.getString(R.string.special_text_is_encoded_as_utf_8_bytes_each_byte_has_eight_bits_letters_a)
        TranslateMode.BINARY_TO_TEXT -> context.getString(R.string.special_enter_whitespace_separated_utf_8_bytes_for_example_01001000_01101)
    }
    val codeOutput = selectedMode == TranslateMode.TEXT_TO_MORSE || selectedMode == TranslateMode.TEXT_TO_BINARY

    val history = rememberToolHistory(
        route = Tools.ROUTE_MORSE_BINARY,
        inputs = mapOf("mode" to mode, "input" to input, "_partial" to (warning != null).toString()),
        output = output?.let { if (warning == null) it else context.getString(R.string.special_partial_output, it, warning) },
        modeInputs = mapOf("mode" to mode),
        onRestoreMode = { saved ->
            mode = TranslateMode.entries.firstOrNull { it.name == saved["mode"] }?.name ?: TranslateMode.TEXT_TO_MORSE.name
        },
        onRestore = { saved ->
            mode = TranslateMode.entries.firstOrNull { it.name == saved["mode"] }?.name ?: TranslateMode.TEXT_TO_MORSE.name
            input = saved["input"] ?: ""
        },
    )

    ToolScaffold(
        title = context.getString(R.string.special_morse_binary_translator),
        icon = Tools.byRoute(Tools.ROUTE_MORSE_BINARY)!!.icon,
        onNavigateBack = onNavigateBack,
        history = history,
    ) {
        CompactChoiceSelector(
            label = context.getString(R.string.special_direction), selected = selectedMode, choices = TranslateMode.entries,
            choiceLabel = { context.getString(it.labelRes) }, onSelect = { if (mode != it.name) input = ""; mode = it.name },
        )

        Spacer(Modifier.height(12.dp))
        Text(guidance, style = MaterialTheme.typography.bodyMedium)
        Text(context.getString(R.string.special_changing_direction_clears_the_input), style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text(context.getString(selectedMode.inputHintRes)) },
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
            Text(if (warning == null) context.getString(R.string.special_translation) else context.getString(R.string.special_partial_translation), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            ResultCard(
                text = output,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = if (codeOutput) FontFamily.Monospace else FontFamily.Default),
                onCopy = { context.copyToClipboard(context.getString(R.string.special_translation), output) },
                onShare = { context.shareText(output) },
            )
        } else if (input.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                if (selectedMode == TranslateMode.BINARY_TO_TEXT) context.getString(R.string.special_enter_valid_utf_8_binary_bytes_each_token_must_contain_1_8_zeros_)
                else context.getString(R.string.special_no_supported_characters_could_be_translated),
                color = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { input = "" }, enabled = input.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Clear, contentDescription = null)
            Text(context.getString(R.string.special_clear))
        }
    }
}
